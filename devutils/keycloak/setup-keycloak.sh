#!/usr/bin/env bash
# =============================================================================
# setup-keycloak.sh
#
# Configures the Keycloak instance used for local development of the
# dealership-ai platform.  Run this once after "docker compose up -d" and
# after the keycloak container reports healthy.
#
# Prerequisites: curl, jq
#
# Usage:
#   ./devutils/keycloak/setup-keycloak.sh
#
# This script is IDEMPOTENT — running it multiple times will not create
# duplicate realms, roles, clients, or users.  Resources that already exist
# are left untouched.
# =============================================================================

set -euo pipefail

# ─────────────────────────────────────────────────────────────────────────────
# Configuration — change these only if you modified the docker-compose values.
# ─────────────────────────────────────────────────────────────────────────────
KEYCLOAK_URL="http://localhost:8180"
ADMIN_USER="admin"
ADMIN_PASSWORD="admin"
REALM="dealership"

# ─────────────────────────────────────────────────────────────────────────────
# Helpers
# ─────────────────────────────────────────────────────────────────────────────
log()  { echo "[setup-keycloak] $*"; }
fail() { echo "[setup-keycloak] ERROR: $*" >&2; exit 1; }

# Executes a Keycloak Admin REST API call.
# Usage: kc <METHOD> <path> [curl-extra-args...]
kc() {
  local method="$1"; shift
  local path="$1";   shift
  curl -fsS -X "$method" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H "Content-Type: application/json" \
    "${KEYCLOAK_URL}/admin/${path}" \
    "$@"
}

# Returns the HTTP status code of a GET request without failing on 4xx/5xx.
kc_status() {
  local path="$1"; shift
  curl -o /dev/null -w "%{http_code}" -s \
    -H "Authorization: Bearer ${TOKEN}" \
    "${KEYCLOAK_URL}/admin/${path}" \
    "$@"
}

# ─────────────────────────────────────────────────────────────────────────────
# 1. Authenticate as the master-realm admin
#    We use the admin-cli client (built-in) with direct-access grant against
#    the master realm to obtain a short-lived admin token.
# ─────────────────────────────────────────────────────────────────────────────
log "Authenticating as ${ADMIN_USER} ..."
TOKEN=$(curl -fsS -X POST \
  "${KEYCLOAK_URL}/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" \
  -d "username=${ADMIN_USER}" \
  -d "password=${ADMIN_PASSWORD}" \
  | jq -r '.access_token')
[[ -z "$TOKEN" || "$TOKEN" == "null" ]] && fail "Failed to obtain admin token."
log "Admin token obtained."

# ─────────────────────────────────────────────────────────────────────────────
# 2. Create the 'dealership' realm (idempotent)
#
#    The realm groups all dealership resources under a single namespace.
#    accessTokenLifespan is set to 1 hour (3600 s) — comfortable for local
#    development without constant re-logins.
# ─────────────────────────────────────────────────────────────────────────────
log "Checking realm '${REALM}' ..."
REALM_STATUS=$(kc_status "realms/${REALM}")
if [[ "$REALM_STATUS" == "404" ]]; then
  log "Creating realm '${REALM}' ..."
  kc POST "realms" -d @- <<EOF
{
  "realm": "${REALM}",
  "displayName": "Dealership",
  "enabled": true,
  "accessTokenLifespan": 3600,
  "ssoSessionMaxLifespan": 36000,
  "verifyEmail": true,
  "smtpServer": {
    "host": "smtp4dev-ai",
    "port": "25",
    "from": "noreply@example.com",
    "fromDisplayName": "Dealership Platform",
    "auth": "false",
    "ssl": "false",
    "starttls": "false"
  }
}
EOF
  log "Realm '${REALM}' created."
else
  log "Realm '${REALM}' already exists — updating SMTP config ..."
  kc PUT "realms/${REALM}" -d @- <<EOF
{
  "realm": "${REALM}",
  "verifyEmail": true,
  "smtpServer": {
    "host": "smtp4dev-ai",
    "port": "25",
    "from": "noreply@example.com",
    "fromDisplayName": "Dealership Platform",
    "auth": "false",
    "ssl": "false",
    "starttls": "false"
  }
}
EOF
  log "SMTP config updated."
fi

# ─────────────────────────────────────────────────────────────────────────────
# 4. Create realm-level roles (idempotent)
#
#    Roles are created WITHOUT the 'ROLE_' prefix.  Both Spring Security
#    converters prepend 'ROLE_' when building GrantedAuthority objects, so:
#      Keycloak role 'CLIENT'  → Spring authority 'ROLE_CLIENT'  ✓
#      Keycloak role 'ADMIN'   → Spring authority 'ROLE_ADMIN'   ✓
#      Keycloak role 'STAFF'   → Spring authority 'ROLE_STAFF'   ✓
#      Keycloak role 'SYSTEM'  → Spring authority 'ROLE_SYSTEM'  ✓
#
#    If the roles were stored as 'ROLE_CLIENT' etc., the client-api converter
#    would double-prefix them → 'ROLE_ROLE_CLIENT', breaking @PreAuthorize.
#
#    Role descriptions:
#      CLIENT  — registered end-user who can manage their own profile
#      ADMIN   — administrator with elevated privileges across all resources
#      STAFF   — dealership employee who manages the car inventory
#      SYSTEM  — internal service account for M2M calls (dealership-system)
# ─────────────────────────────────────────────────────────────────────────────
declare -A ROLE_DESCRIPTIONS=(
  [CLIENT]="Registered end-user; can manage their own client profile"
  [ADMIN]="Administrator; elevated privileges across all resources"
  [STAFF]="Dealership staff; manages the car inventory"
  [SYSTEM]="Internal service account for machine-to-machine calls"
)

for ROLE_NAME in CLIENT ADMIN STAFF SYSTEM; do
  ROLE_STATUS=$(kc_status "realms/${REALM}/roles/${ROLE_NAME}")
  if [[ "$ROLE_STATUS" == "404" ]]; then
    log "Creating realm role '${ROLE_NAME}' ..."
    kc POST "realms/${REALM}/roles" -d @- <<EOF
{
  "name": "${ROLE_NAME}",
  "description": "${ROLE_DESCRIPTIONS[$ROLE_NAME]}"
}
EOF
    log "Role '${ROLE_NAME}' created."
  else
    log "Role '${ROLE_NAME}' already exists — skipping."
  fi
done

# ─────────────────────────────────────────────────────────────────────────────
# 5. Create a shared client scope that injects the 'dealership' audience
#
#    Both the BFF client and the system client include this scope so that
#    every token they issue contains:  "aud": [..., "dealership"]
#
#    The Spring Security resource servers in car-api and client-api validate
#    this claim and reject tokens that don't list 'dealership' as an audience,
#    preventing tokens issued for unrelated clients from being accepted.
# ─────────────────────────────────────────────────────────────────────────────
SCOPE_NAME="dealership-audience"
log "Checking client scope '${SCOPE_NAME}' ..."

EXISTING_SCOPE_ID=$(kc GET "realms/${REALM}/client-scopes" \
  | jq -r --arg name "$SCOPE_NAME" '.[] | select(.name==$name) | .id' || true)

if [[ -z "$EXISTING_SCOPE_ID" ]]; then
  log "Creating client scope '${SCOPE_NAME}' ..."
  kc POST "realms/${REALM}/client-scopes" -d @- <<EOF
{
  "name": "${SCOPE_NAME}",
  "description": "Adds the 'dealership' audience to every issued token",
  "protocol": "openid-connect",
  "attributes": {
    "include.in.token.scope": "false",
    "display.on.consent.screen": "false"
  }
}
EOF
  # Re-fetch the ID after creation
  EXISTING_SCOPE_ID=$(kc GET "realms/${REALM}/client-scopes" \
    | jq -r --arg name "$SCOPE_NAME" '.[] | select(.name==$name) | .id')
  log "Client scope '${SCOPE_NAME}' created (id=${EXISTING_SCOPE_ID})."

  # Add a Hardcoded Audience mapper to the scope
  log "Adding audience mapper to scope '${SCOPE_NAME}' ..."
  kc POST "realms/${REALM}/client-scopes/${EXISTING_SCOPE_ID}/protocol-mappers/models" -d @- <<EOF
{
  "name": "dealership-audience-mapper",
  "protocol": "openid-connect",
  "protocolMapper": "oidc-audience-mapper",
  "consentRequired": false,
  "config": {
    "included.custom.audience": "dealership",
    "id.token.claim": "false",
    "access.token.claim": "true"
  }
}
EOF
  log "Audience mapper added."
else
  log "Client scope '${SCOPE_NAME}' already exists (id=${EXISTING_SCOPE_ID}) — skipping."
fi

# ─────────────────────────────────────────────────────────────────────────────
# Helper: look up client UUID by clientId
# ─────────────────────────────────────────────────────────────────────────────
get_client_uuid() {
  local client_id="$1"
  kc GET "realms/${REALM}/clients?clientId=${client_id}" \
    | jq -r '.[0].id // empty'
}

# ─────────────────────────────────────────────────────────────────────────────
# Helper: assign the dealership-audience scope to a client as a default scope
# ─────────────────────────────────────────────────────────────────────────────
assign_audience_scope() {
  local client_uuid="$1"
  # Check if already assigned
  ASSIGNED=$(kc GET "realms/${REALM}/clients/${client_uuid}/default-client-scopes" \
    | jq -r --arg sid "$EXISTING_SCOPE_ID" '.[] | select(.id==$sid) | .id' || true)
  if [[ -z "$ASSIGNED" ]]; then
    kc PUT "realms/${REALM}/clients/${client_uuid}/default-client-scopes/${EXISTING_SCOPE_ID}" \
      -d '{}' > /dev/null
    log "  Audience scope assigned."
  else
    log "  Audience scope already assigned — skipping."
  fi
}

# ─────────────────────────────────────────────────────────────────────────────
# 6. Create the BFF client: dealership-bff  (idempotent)
#
#    Used by the Backend-For-Frontend to drive the browser login flow on
#    behalf of the end-user (standard OAuth2 Authorization Code Flow).
#
#    - confidential (publicClient: false) — requires a client secret
#    - standardFlowEnabled: true          — Authorization Code Flow
#    - directAccessGrantsEnabled: false   — Resource Owner Password disabled
#    - serviceAccountsEnabled: false      — no M2M token for this client
#
#    Redirect URIs and web origins are set wide for localhost dev convenience.
# ─────────────────────────────────────────────────────────────────────────────
BFF_CLIENT_ID="dealership-bff"
BFF_CLIENT_SECRET="dealership-bff-secret"
log "Checking client '${BFF_CLIENT_ID}' ..."
BFF_UUID=$(get_client_uuid "$BFF_CLIENT_ID")

if [[ -z "$BFF_UUID" ]]; then
  log "Creating client '${BFF_CLIENT_ID}' ..."
  kc POST "realms/${REALM}/clients" -d @- <<EOF
{
  "clientId": "${BFF_CLIENT_ID}",
  "name": "Dealership BFF",
  "description": "Backend-For-Frontend — handles the browser login flow",
  "enabled": true,
  "publicClient": false,
  "standardFlowEnabled": true,
  "directAccessGrantsEnabled": false,
  "serviceAccountsEnabled": false,
  "redirectUris": [
    "http://localhost:*",
    "http://127.0.0.1:*"
  ],
  "webOrigins": ["+"]
}
EOF
  BFF_UUID=$(get_client_uuid "$BFF_CLIENT_ID")
  log "Client '${BFF_CLIENT_ID}' created (uuid=${BFF_UUID})."
else
  log "Client '${BFF_CLIENT_ID}' already exists (uuid=${BFF_UUID}) — skipping."
fi

# Always enforce the known secret so local dev works without manual copy-paste.
log "Setting secret for '${BFF_CLIENT_ID}' ..."
kc PUT "realms/${REALM}/clients/${BFF_UUID}" -d @- <<EOF > /dev/null
{"secret": "${BFF_CLIENT_SECRET}"}
EOF
log "Secret set."

log "Assigning audience scope to '${BFF_CLIENT_ID}' ..."
assign_audience_scope "$BFF_UUID"

# ─────────────────────────────────────────────────────────────────────────────
# 7. Create the system client: dealership-system  (idempotent)
#
#    Used by internal services for machine-to-machine (M2M) calls.
#    A token obtained via Client Credentials Flow will carry the SYSTEM role,
#    allowing callers to hit protected internal endpoints on the client-api
#    (e.g. PATCH /clients/{id} which accepts ROLE_SYSTEM).
#
#    - confidential (publicClient: false)
#    - standardFlowEnabled: false        — no browser login
#    - serviceAccountsEnabled: true      — enables Client Credentials Flow
#    - directAccessGrantsEnabled: false
# ─────────────────────────────────────────────────────────────────────────────
SYS_CLIENT_ID="dealership-system"
SYS_CLIENT_SECRET="dealership-system-secret"
log "Checking client '${SYS_CLIENT_ID}' ..."
SYS_UUID=$(get_client_uuid "$SYS_CLIENT_ID")

if [[ -z "$SYS_UUID" ]]; then
  log "Creating client '${SYS_CLIENT_ID}' ..."
  kc POST "realms/${REALM}/clients" -d @- <<EOF
{
  "clientId": "${SYS_CLIENT_ID}",
  "name": "Dealership System",
  "description": "Internal service account for M2M calls between platform services",
  "enabled": true,
  "publicClient": false,
  "standardFlowEnabled": false,
  "directAccessGrantsEnabled": false,
  "serviceAccountsEnabled": true
}
EOF
  SYS_UUID=$(get_client_uuid "$SYS_CLIENT_ID")
  log "Client '${SYS_CLIENT_ID}' created (uuid=${SYS_UUID})."
else
  log "Client '${SYS_CLIENT_ID}' already exists (uuid=${SYS_UUID}) — skipping."
fi

# Always enforce the known secret — POST /client-secret regenerates a random
# secret and ignores the payload; the correct way to set a specific secret is
# a PUT on the client resource itself.
log "Setting secret for '${SYS_CLIENT_ID}' ..."
kc PUT "realms/${REALM}/clients/${SYS_UUID}" -d @- <<EOF > /dev/null
{"secret": "${SYS_CLIENT_SECRET}"}
EOF
log "Secret set."

log "Assigning audience scope to '${SYS_CLIENT_ID}' ..."
assign_audience_scope "$SYS_UUID"

SA_USER_ID=$(kc GET "realms/${REALM}/clients/${SYS_UUID}/service-account-user" \
  | jq -r '.id')

# Assign the SYSTEM realm role to the service account.
log "Assigning SYSTEM role to '${SYS_CLIENT_ID}' service account ..."
SYSTEM_ROLE=$(kc GET "realms/${REALM}/roles/SYSTEM")
ALREADY_ASSIGNED=$(kc GET "realms/${REALM}/users/${SA_USER_ID}/role-mappings/realm" \
  | jq -r '.[] | select(.name=="SYSTEM") | .name' || true)
if [[ -z "$ALREADY_ASSIGNED" ]]; then
  kc POST "realms/${REALM}/users/${SA_USER_ID}/role-mappings/realm" \
    -d "[${SYSTEM_ROLE}]" > /dev/null
  log "  SYSTEM role assigned to service account."
else
  log "  SYSTEM role already assigned to service account — skipping."
fi

# ─────────────────────────────────────────────────────────────────────────────
# 8. Create test users  (idempotent)
#
#    One user per role, with predictable credentials for local development.
#    Passwords are set as non-temporary so no password-change prompt appears
#    on first login.
#
#    | username    | password    | realm role |
#    |-------------|-------------|------------|
#    | client-user | client-pass | CLIENT     |
#    | admin-user  | admin-pass  | ADMIN      |
#    | staff-user  | staff-pass  | STAFF      |
# ─────────────────────────────────────────────────────────────────────────────

# Helper: create a user, set their password, and assign a realm role.
# Idempotent: does nothing if the username already exists.
create_test_user() {
  local username="$1"
  local password="$2"
  local role_name="$3"

  log "Checking user '${username}' ..."
  USER_ID=$(kc GET "realms/${REALM}/users?username=${username}&exact=true" \
    | jq -r '.[0].id // empty')

  if [[ -z "$USER_ID" ]]; then
    log "Creating user '${username}' ..."
    kc POST "realms/${REALM}/users" -d @- <<EOF
{
  "username": "${username}",
  "enabled": true,
  "emailVerified": true,
  "firstName": "${username}",
  "lastName": "Test",
  "email": "${username}@dealership.local"
}
EOF
    USER_ID=$(kc GET "realms/${REALM}/users?username=${username}&exact=true" \
      | jq -r '.[0].id')
    log "  User '${username}' created (id=${USER_ID})."

    # Set a non-temporary password so the user can log in immediately
    kc PUT "realms/${REALM}/users/${USER_ID}/reset-password" -d @- <<EOF
{
  "type": "password",
  "value": "${password}",
  "temporary": false
}
EOF
    log "  Password set."
  else
    log "  User '${username}' already exists (id=${USER_ID}) — skipping creation."
  fi

  # Assign the realm role (idempotent check)
  ROLE_ASSIGNED=$(kc GET "realms/${REALM}/users/${USER_ID}/role-mappings/realm" \
    | jq -r --arg r "$role_name" '.[] | select(.name==$r) | .name' || true)

  if [[ -z "$ROLE_ASSIGNED" ]]; then
    ROLE_REP=$(kc GET "realms/${REALM}/roles/${role_name}")
    kc POST "realms/${REALM}/users/${USER_ID}/role-mappings/realm" \
      -d "[${ROLE_REP}]" > /dev/null
    log "  Role '${role_name}' assigned to '${username}'."
  else
    log "  Role '${role_name}' already assigned to '${username}' — skipping."
  fi
}

create_test_user "client-user" "client-pass" "CLIENT"
create_test_user "admin-user"  "admin-pass"  "ADMIN"
create_test_user "staff-user"  "staff-pass"  "STAFF"

# ─────────────────────────────────────────────────────────────────────────────
# Done
# ─────────────────────────────────────────────────────────────────────────────
log ""
log "Keycloak configuration complete."
log ""
log "  Realm      : ${REALM}"
log "  Admin UI   : ${KEYCLOAK_URL}/admin"
log "  OIDC base  : ${KEYCLOAK_URL}/realms/${REALM}"
log "  JWKS URI   : ${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/certs"
log ""
log "  Test users :"
log "    client-user / client-pass  (ROLE_CLIENT)"
log "    admin-user  / admin-pass   (ROLE_ADMIN)"
log "    staff-user  / staff-pass   (ROLE_STAFF)"
log ""
log "  Clients    :"
log "    dealership-bff    — Authorization Code Flow (confidential, secret=${BFF_CLIENT_SECRET})"
log "    dealership-system — Client Credentials Flow (service account, secret=${SYS_CLIENT_SECRET})"
log ""
log "  To get a token for manual testing:"
log "    curl -s '${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token' \\"
log "      -d 'grant_type=password' \\"
log "      -d 'client_id=dealership-bff' \\"
log "      -d 'client_secret=${BFF_CLIENT_SECRET}' \\"
log "      -d 'username=staff-user' \\"
log "      -d 'password=staff-pass' | jq .access_token"
