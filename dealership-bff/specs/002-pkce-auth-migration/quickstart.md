# Quickstart: PKCE Auth Migration — Local Development

**Feature**: `002-pkce-auth-migration`  
**Phase**: Phase 1 — Design  
**Date**: 2025-07-15

---

## Prerequisites

- Java 25
- Maven 3.9+
- Docker (for Redis + Keycloak via `compose.yaml`)
- A Keycloak realm named `dealership` with:
  - A client `dealership-bff` configured for `authorization_code` grant with PKCE
  - Valid redirect URI: `http://localhost:8083/login/oauth2/code/keycloak`
  - Valid post-logout redirect URI: `http://localhost:3000` (or your frontend URL)
  - PKCE enforced (S256)

---

## Keycloak Client Configuration (deployment prerequisite)

In the Keycloak admin console for realm `dealership`, client `dealership-bff`:

1. **Access Type / Client Authentication**: `public` (no client secret for PKCE public client)  
   OR `confidential` with PKCE required (depends on Keycloak version — for Keycloak 21+ use `Standard flow` + PKCE enforced)
2. **Standard Flow**: Enabled
3. **Direct Access Grants (ROPC)**: **Disabled** — this is the old flow being removed
4. **Valid Redirect URIs**: `http://localhost:8083/login/oauth2/code/keycloak`
5. **Valid Post Logout Redirect URIs**: `http://localhost:3000`
6. **Web Origins**: `http://localhost:8083`

---

## Local Run

### 1. Start infrastructure

```bash
cd dealership-bff
docker compose up -d   # starts Redis and Keycloak
```

### 2. Configure application.properties (local overrides)

For local development, you may need to relax the cookie Secure flag:

```properties
# Local dev only — disable Secure flag so cookie works over HTTP
server.servlet.session.cookie.secure=false
```

### 3. Build and run

```bash
./mvnw spring-boot:run
```

The BFF starts on port 8083.

---

## Testing the Login Flow

### Browser-based (recommended)

1. Open `http://localhost:8083/oauth2/authorization/keycloak`
2. You are redirected to Keycloak's login page
3. Enter credentials → Keycloak redirects back to BFF callback
4. BFF completes code exchange → session created → `SESSION` cookie set
5. Browser redirected to `app.post-login-redirect-uri`

**Verify** (browser DevTools → Application → Cookies):
- `SESSION` cookie is present: `HttpOnly ✓`, `Secure` depends on env
- No `access_token` or `refresh_token` cookies visible
- localStorage/sessionStorage: empty (no tokens)

### API-level (curl)

```bash
# Step 1: Initiate login — get the Keycloak redirect URL
curl -v -L -c cookies.txt http://localhost:8083/oauth2/authorization/keycloak 2>&1 | grep Location

# Step 2: Complete login in browser (required for Keycloak credential entry)
# Copy the SESSION cookie value from cookies.txt after login

# Step 3: Make an authenticated API call
curl -v -H "Cookie: SESSION=<session-value>" \
     http://localhost:8083/api/v1/profile/me
```

---

## Testing Logout

```bash
# Logout — invalidates session and initiates Keycloak OIDC end_session
curl -v -X POST \
     -H "Cookie: SESSION=<session-value>" \
     http://localhost:8083/api/v1/auth/logout

# Verify old session is invalid (expect 401)
curl -v -H "Cookie: SESSION=<old-session-value>" \
     http://localhost:8083/api/v1/profile/me
```

---

## Testing Registration (unchanged)

```bash
curl -v -X POST http://localhost:8083/api/v1/auth/register \
     -H "Content-Type: application/json" \
     -d '{
       "email": "test@dealership.com",
       "password": "Test1234!",
       "firstName": "João",
       "lastName": "Silva",
       "cpf": "123.456.789-09",
       "phone": "+5511999999999",
       "cep": "01310-100",
       "streetNumber": "42"
     }'
# Expect: 201 Created with ApiResponse<ClientApiClientResponse>
```

---

## New application.properties Keys

```properties
# ─── OAuth2 Client (PKCE) ─────────────────────────────────────────────────────
spring.security.oauth2.client.registration.keycloak.client-id=${KEYCLOAK_CLIENT_ID:dealership-bff}
spring.security.oauth2.client.registration.keycloak.authorization-grant-type=authorization_code
spring.security.oauth2.client.registration.keycloak.scope=openid,profile,email
spring.security.oauth2.client.registration.keycloak.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}

spring.security.oauth2.client.provider.keycloak.issuer-uri=${KEYCLOAK_BASE_URL:http://localhost:8080}/realms/${KEYCLOAK_REALM:dealership}

# ─── Spring Session (Redis) ───────────────────────────────────────────────────
spring.session.store-type=redis
spring.session.redis.flush-mode=on-save
spring.session.redis.namespace=spring:session:bff

# ─── Session Cookie ───────────────────────────────────────────────────────────
server.servlet.session.timeout=${SESSION_TIMEOUT:1800s}
server.servlet.session.cookie.name=SESSION
server.servlet.session.cookie.http-only=true
server.servlet.session.cookie.secure=${SESSION_COOKIE_SECURE:true}
server.servlet.session.cookie.same-site=lax
server.servlet.session.cookie.path=/

# ─── App Redirect URIs ────────────────────────────────────────────────────────
app.post-login-redirect-uri=${APP_POST_LOGIN_REDIRECT_URI:http://localhost:3000/dashboard}
app.post-logout-redirect-uri=${APP_POST_LOGOUT_REDIRECT_URI:http://localhost:3000/login}
```

---

## Integration Test Containers Required

- **Redis** (`org.testcontainers:testcontainers` — Testcontainers GenericContainer or `RedisContainer`)
- **Keycloak** (`com.github.dasniko:testcontainers-keycloak` or WireMock stubs for OIDC endpoints)

For unit tests of `SessionTokenInjectionFilter` and `OAuth2LoginSuccessHandler`,
use Mockito to mock `HttpSession`, `OAuth2AuthorizedClientManager`, and
`HttpServletRequest`.
