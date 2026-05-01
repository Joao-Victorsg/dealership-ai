# Data Model: PKCE Auth Migration

**Feature**: `002-pkce-auth-migration`  
**Phase**: Phase 1 — Design  
**Date**: 2025-07-15

---

## Overview

This feature does not introduce new database entities or REST resources. The
core model change is the replacement of the stateless JWT-in-cookie pattern
with a **server-side session** whose opaque ID is surfaced to the browser via
an HttpOnly cookie. All token data lives exclusively in the server-side session
store (Redis).

---

## Entity: Session

Stored in Redis via `spring-session-data-redis`. Identified by a UUID session
ID that is placed in an HttpOnly session cookie named `SESSION`.

### Session Attributes

| Attribute key | Type | Description |
|---|---|---|
| `bff.access_token` | `String` | JWT access token issued by Keycloak |
| `bff.refresh_token` | `String` | Opaque refresh token issued by Keycloak |
| `bff.id_token` | `String` | OIDC ID token (required for `id_token_hint` on logout) |
| `bff.token_expiry` | `Instant` | Expiry instant of the access token (parsed from JWT `exp` claim) |
| `SPRING_SECURITY_CONTEXT` | `SecurityContext` | Spring's authenticated security context (set by Spring Security) |
| Spring OAuth2 client attributes | `OAuth2AuthorizedClient` | Managed by `HttpSessionOAuth2AuthorizedClientRepository` |

### Session Lifecycle

```
[Browser] ──GET /oauth2/authorization/keycloak──► [BFF]
               Spring creates transient session for state + code_verifier storage

[Keycloak] ──redirect with code──► [BFF /login/oauth2/code/keycloak]
               Spring completes code exchange
               OAuth2LoginSuccessHandler writes bff.* attributes
               Session becomes authenticated

[Browser] ──API request + SESSION cookie──► [BFF]
               SessionTokenInjectionFilter reads session
               Injects Authorization: Bearer <access_token>
               If expired: OAuth2AuthorizedClientManager refreshes
               bff.* attributes updated with new tokens

[Browser] ──POST /api/v1/auth/logout──► [BFF]
               AuthService reads id_token from session
               Session invalidated + SESSION cookie cleared
               Redirect to Keycloak end_session_endpoint

Session expires (Redis TTL) ──► 401 on next request
```

### Validation Rules

- `bff.access_token` must be non-null and non-blank on every authenticated request.
- `bff.token_expiry` compared to `Instant.now()` before downstream calls.
- If `bff.refresh_token` is null/blank and access token is expired → session is
  invalid → HTTP 401, session invalidated.
- Redis session TTL must be ≥ Keycloak's SSO session max lifetime. Configure via
  `server.servlet.session.timeout`.

---

## Entity: PKCE Flow State (Transient)

Managed entirely by Spring Security internals. Not accessible to application code.
Stored in the HttpSession during the login redirect window.

| Attribute | Description |
|---|---|
| `code_verifier` | Random 43-128 char Base64url string; never leaves server |
| `code_challenge` | SHA-256 hash of code_verifier, sent to Keycloak |
| `state` | Random string; sent to Keycloak, validated at callback |
| `redirect_uri` | BFF callback URL registered with Keycloak |

These attributes are written by `OAuth2AuthorizationRequestRedirectFilter` and
read by `OAuth2LoginAuthenticationFilter` on callback. They are discarded after
successful code exchange.

---

## Removed Models

The following DTOs are deleted as part of this migration:

### `dto/request/LoginRequest.java` (DELETED)

Previously:
```java
public record LoginRequest(
    @NotBlank String email,
    @NotBlank String password
) {}
```
Deleted because: passwords no longer flow through the BFF. Keycloak hosts the
login form.

### `dto/response/TokenResponse.java` (DELETED)

Previously:
```java
public record TokenResponse(String accessToken) {
    public static TokenResponse of(String token) { ... }
}
```
Deleted because: the BFF never returns tokens to the frontend. The session
cookie is the only credential the browser receives.

---

## Cookie Model

### Before (ROPC)

| Cookie | Content | Visible to JS |
|---|---|---|
| `refresh_token` | Opaque Keycloak refresh token | No (HttpOnly) |
| _(none)_ | Access token returned in response body | **Yes** |

### After (PKCE + Spring Session)

| Cookie | Content | Visible to JS |
|---|---|---|
| `SESSION` | Opaque session ID (UUID) | No (HttpOnly) |

The `SESSION` cookie is configured as:
- `HttpOnly: true`
- `Secure: true` (disable only in local dev via `server.servlet.session.cookie.secure=false`)
- `SameSite: Lax` (Spring Session default; `Strict` may break Keycloak post-login redirect)
- `Path: /`
- Max-age: driven by `server.servlet.session.timeout` (default 30 min)
