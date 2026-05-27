#!/usr/bin/env bash

set -euo pipefail

log()  { echo "[seed-data] $*" >&2; }
fail() { echo "[seed-data] ERROR: $*" >&2; exit 1; }
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

require_cmd() {
  local cmd="$1"
  command -v "$cmd" >/dev/null 2>&1 || fail "Missing required command: $cmd"
}

wait_for_url() {
  local name="$1"
  local url="$2"
  local retries="${3:-40}"
  local sleep_seconds="${4:-2}"

  for i in $(seq 1 "$retries"); do
    if curl -fsS "$url" >/dev/null 2>&1; then
      log "${name} is healthy (${url})"
      return 0
    fi
    sleep "$sleep_seconds"
  done

  fail "${name} did not become healthy (${url})"
}

# -----------------------------------------------------------------------------
# Defaults for local development
# -----------------------------------------------------------------------------
KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8180}"
KEYCLOAK_ADMIN_USER="${KEYCLOAK_ADMIN_USER:-admin}"
KEYCLOAK_ADMIN_PASSWORD="${KEYCLOAK_ADMIN_PASSWORD:-admin}"
REALM="${REALM:-dealership}"

CLIENT_API_URL="${CLIENT_API_URL:-http://localhost:8081}"
CAR_API_HEALTH_URL="${CAR_API_HEALTH_URL:-http://localhost:8080/actuator/health}"
CLIENT_API_HEALTH_URL="${CLIENT_API_HEALTH_URL:-http://localhost:8081/actuator/health}"

SYSTEM_CLIENT_ID="${SYSTEM_CLIENT_ID:-dealership-system}"
SYSTEM_CLIENT_SECRET="${SYSTEM_CLIENT_SECRET:-dealership-system-secret}"

CAR_DB_HOST="${CAR_DB_HOST:-aws-dealership-ai}"
CAR_DB_PORT="${CAR_DB_PORT:-4510}"
CAR_DB_NAME="${CAR_DB_NAME:-dealershipdb}"
CAR_DB_USER="${CAR_DB_USER:-dealership}"
CAR_DB_PASSWORD="${CAR_DB_PASSWORD:-changeme}"

SEED_CLIENT_USERNAME="${SEED_CLIENT_USERNAME:-seed-client}"
SEED_CLIENT_PASSWORD="${SEED_CLIENT_PASSWORD:-seed-client-pass}"
SEED_ADMIN_USERNAME="${SEED_ADMIN_USERNAME:-seed-admin}"
SEED_ADMIN_PASSWORD="${SEED_ADMIN_PASSWORD:-seed-admin-pass}"

SEED_CLIENT_FIRST_NAME="${SEED_CLIENT_FIRST_NAME:-Camila}"
SEED_CLIENT_LAST_NAME="${SEED_CLIENT_LAST_NAME:-Santos}"
SEED_CLIENT_EMAIL="${SEED_CLIENT_EMAIL:-seed-client@dealership.local}"
SEED_CLIENT_CPF="${SEED_CLIENT_CPF:-529.982.247-25}"
SEED_CLIENT_PHONE="${SEED_CLIENT_PHONE:-+55 (11) 99876-5432}"
SEED_CLIENT_POSTCODE="${SEED_CLIENT_POSTCODE:-01001000}"
SEED_CLIENT_STREET_NUMBER="${SEED_CLIENT_STREET_NUMBER:-120}"

kc() {
  local method="$1"; shift
  local path="$1"; shift

  curl -fsS -X "$method" \
    -H "Authorization: Bearer ${KC_ADMIN_TOKEN}" \
    -H "Content-Type: application/json" \
    "${KEYCLOAK_URL}/admin/${path}" \
    "$@"
}

get_user_id_by_username() {
  local username="$1"
  kc GET "realms/${REALM}/users?username=${username}&exact=true" \
    | jq -r '.[0].id // empty'
}

ensure_user_with_role() {
  local username="$1"
  local password="$2"
  local role_name="$3"
  local first_name="$4"
  local last_name="$5"
  local email="$6"

  local user_id
  user_id="$(get_user_id_by_username "$username")"

  if [[ -z "$user_id" ]]; then
    log "Creating Keycloak user '${username}' ..."
    kc POST "realms/${REALM}/users" -d @- <<EOF
{
  "username": "${username}",
  "enabled": true,
  "emailVerified": true,
  "firstName": "${first_name}",
  "lastName": "${last_name}",
  "email": "${email}"
}
EOF
    user_id="$(get_user_id_by_username "$username")"
    [[ -z "$user_id" ]] && fail "Failed to create user '${username}'"
  else
    log "Keycloak user '${username}' already exists (id=${user_id})"
  fi

  # Ensure known password for local development.
  kc PUT "realms/${REALM}/users/${user_id}/reset-password" -d @- <<EOF >/dev/null
{
  "type": "password",
  "value": "${password}",
  "temporary": false
}
EOF

  local role_assigned
  role_assigned="$(kc GET "realms/${REALM}/users/${user_id}/role-mappings/realm" \
    | jq -r --arg role "$role_name" '.[] | select(.name==$role) | .name' || true)"
  if [[ -z "$role_assigned" ]]; then
    local role_rep
    role_rep="$(kc GET "realms/${REALM}/roles/${role_name}")"
    kc POST "realms/${REALM}/users/${user_id}/role-mappings/realm" -d "[${role_rep}]" >/dev/null
    log "Assigned role '${role_name}' to '${username}'"
  fi

  echo "$user_id"
}

seed_car_rows() {
  log "Seeding 'car' table rows ..."
  local sql_file="${SCRIPT_DIR}/sql/seed-cars.sql"
  [[ -f "$sql_file" ]] || fail "Missing SQL seed file: ${sql_file}"

  cat "$sql_file" | docker run --rm --network dealership-ai-network \
    -i \
    -e PGPASSWORD="${CAR_DB_PASSWORD}" \
    postgres:17-alpine \
    psql \
      -h "${CAR_DB_HOST}" \
      -p "${CAR_DB_PORT}" \
      -U "${CAR_DB_USER}" \
      -d "${CAR_DB_NAME}" \
      -v ON_ERROR_STOP=1
}

create_client_profile() {
  local keycloak_user_id="$1"

  log "Creating seed client profile in client-api (idempotent) ..."

  local system_token
  system_token="$(
    curl -fsS -X POST \
      "${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token" \
      -H "Content-Type: application/x-www-form-urlencoded" \
      -d "grant_type=client_credentials" \
      -d "client_id=${SYSTEM_CLIENT_ID}" \
      -d "client_secret=${SYSTEM_CLIENT_SECRET}" \
      | jq -r '.access_token'
  )"
  [[ -z "$system_token" || "$system_token" == "null" ]] && fail "Failed to obtain system access token"

  local body_file
  body_file="$(mktemp)"
  trap 'rm -f "$body_file"' EXIT

  local status
  status="$(
    curl -sS \
      -o "$body_file" \
      -w "%{http_code}" \
      -X POST "${CLIENT_API_URL}/clients" \
      -H "Authorization: Bearer ${system_token}" \
      -H "Content-Type: application/json" \
      -d @- <<EOF
{
  "keycloakId": "${keycloak_user_id}",
  "firstName": "${SEED_CLIENT_FIRST_NAME}",
  "lastName": "${SEED_CLIENT_LAST_NAME}",
  "cpf": "${SEED_CLIENT_CPF}",
  "phoneNumber": "${SEED_CLIENT_PHONE}",
  "postcode": "${SEED_CLIENT_POSTCODE}",
  "streetNumber": "${SEED_CLIENT_STREET_NUMBER}"
}
EOF
  )"

  case "$status" in
    201)
      log "Seed client profile created."
      ;;
    422)
      log "Seed client profile already exists (HTTP 422 duplicate)."
      ;;
    *)
      log "client-api response body:"
      cat "$body_file" >&2
      fail "Failed to create seed client profile (HTTP ${status})"
      ;;
  esac

  rm -f "$body_file"
  trap - EXIT
}

main() {
  require_cmd curl
  require_cmd jq
  require_cmd docker

  wait_for_url "Keycloak realm" "${KEYCLOAK_URL}/realms/${REALM}"
  wait_for_url "car-api health" "${CAR_API_HEALTH_URL}"
  wait_for_url "client-api health" "${CLIENT_API_HEALTH_URL}"

  log "Authenticating Keycloak admin ..."
  KC_ADMIN_TOKEN="$(
    curl -fsS -X POST \
      "${KEYCLOAK_URL}/realms/master/protocol/openid-connect/token" \
      -H "Content-Type: application/x-www-form-urlencoded" \
      -d "grant_type=password" \
      -d "client_id=admin-cli" \
      -d "username=${KEYCLOAK_ADMIN_USER}" \
      -d "password=${KEYCLOAK_ADMIN_PASSWORD}" \
      | jq -r '.access_token'
  )"
  [[ -z "$KC_ADMIN_TOKEN" || "$KC_ADMIN_TOKEN" == "null" ]] && fail "Failed to obtain Keycloak admin token"

  local seed_client_id
  seed_client_id="$(ensure_user_with_role \
    "${SEED_CLIENT_USERNAME}" \
    "${SEED_CLIENT_PASSWORD}" \
    "CLIENT" \
    "Seed" \
    "Client" \
    "${SEED_CLIENT_EMAIL}")"

  ensure_user_with_role \
    "${SEED_ADMIN_USERNAME}" \
    "${SEED_ADMIN_PASSWORD}" \
    "ADMIN" \
    "Seed" \
    "Admin" \
    "seed-admin@dealership.local" >/dev/null

  seed_car_rows
  create_client_profile "$seed_client_id"

  log "Sample data seeded successfully."
}

main "$@"
