# Auth Flow Contracts: PKCE Auth Migration

**Feature**: `002-pkce-auth-migration`  
**Phase**: Phase 1 — Design  
**Date**: 2025-07-15

---

## Overview

This document describes the HTTP contracts exposed to the frontend after the
migration. Two endpoints are **removed**, two are **Spring-managed** (new), one
is **updated**, and one is **unchanged**.

All responses that remain under `AuthController` continue to follow the BFF
envelope contract (Article III — Constitution).

---

## Removed Endpoints

### `POST /api/v1/auth/login` — REMOVED

Previously accepted `LoginRequest { email, password }` and returned
`ApiResponse<TokenResponse>`. **Removed entirely.** Login is now initiated by
the browser navigating to `/oauth2/authorization/keycloak`.

### `POST /api/v1/auth/refresh` — REMOVED

Previously accepted the `refresh_token` HttpOnly cookie and returned a new
`ApiResponse<TokenResponse>`. **Removed entirely.** Token refresh is now
performed transparently inside `SessionTokenInjectionFilter` on every
authenticated request.

---

## New Spring-Managed Endpoints (auto-configured, no controller code)

### `GET /oauth2/authorization/keycloak` — Login Initiation

Managed by Spring Security's `OAuth2AuthorizationRequestRedirectFilter`.

**Request**: No body. No authentication required.

**Response**:
- `302 Found`
- `Location: https://<keycloak>/realms/dealership/protocol/openid-connect/auth`  
  `?client_id=dealership-bff`  
  `&response_type=code`  
  `&scope=openid+profile+email`  
  `&redirect_uri=https://<bff>/login/oauth2/code/keycloak`  
  `&state=<random>`  
  `&code_challenge=<S256>`  
  `&code_challenge_method=S256`

**Frontend usage**: The frontend redirects (or links) the user to this URL to
initiate login. No JavaScript token handling required.

---

### `GET /login/oauth2/code/keycloak` — PKCE Callback

Managed by Spring Security's `OAuth2LoginAuthenticationFilter`.

**Request**:
- Query params: `code=<auth_code>&state=<state>`
- Set by Keycloak after successful user authentication.

**Success response**:
- `302 Found`
- `Location: <app.post-login-redirect-uri>` (configured in application.properties)
- `Set-Cookie: SESSION=<opaque-id>; HttpOnly; Secure; SameSite=Lax; Path=/`

**Failure response** (state mismatch, code exchange failure):
- `302 Found`
- `Location: /login?error` (Spring default; configurable)

**Handled by**: `OAuth2LoginSuccessHandler` (called by Spring after exchange
success) — stores `bff.*` session attributes, redirects to post-login URL.

---

## Updated Endpoint

### `POST /api/v1/auth/logout` — Updated

**Authentication**: Requires valid `SESSION` cookie.

**Request**: No body required.

**Processing**:
1. `AuthService.logout(session, response)` reads `bff.id_token` from session.
2. Invalidates the server-side session.
3. Clears the `SESSION` cookie.
4. Spring Security issues redirect to Keycloak `end_session_endpoint` with
   `id_token_hint` and `post_logout_redirect_uri`.

**Success response** (direct API call, no browser redirect):
```
HTTP/1.1 204 No Content
Content-Type: application/json

{
  "data": null,
  "meta": {
    "timestamp": "2025-07-15T10:00:00Z",
    "requestId": "abc-123"
  }
}
```

**Note on logout redirect**: When invoked from a browser (not a pure API call),
Spring Security's `OidcClientInitiatedLogoutSuccessHandler` issues a `302`
redirect to Keycloak's `end_session_endpoint`. The frontend should handle this
redirect. The `post_logout_redirect_uri` is configured via
`app.post-logout-redirect-uri` in application.properties.

**Error response** (no valid session):
```
HTTP/1.1 401 Unauthorized
{
  "error": {
    "code": "AUTHENTICATION_REQUIRED",
    "message": "Authentication is required to access this resource."
  },
  "meta": { "timestamp": "...", "requestId": "..." }
}
```

---

## Unchanged Endpoint

### `POST /api/v1/auth/register` — Unchanged

**No changes** to contract, behaviour, or implementation. Continues to accept
`RegisterRequest` and return `ApiResponse<ClientApiClientResponse>` with 201.
No authentication required.

---

## Authenticated API Calls (All Other Endpoints)

**Before**: Frontend sent `Authorization: Bearer <access_token>` header.

**After**: Frontend sends only the `SESSION` cookie. No `Authorization` header.

The `SessionTokenInjectionFilter` intercepts every request, reads the session,
and injects `Authorization: Bearer <stored_access_token>` into the request
before the `BearerTokenAuthenticationFilter` processes it. The frontend is
completely unaware of this mechanism.

**No changes** to any of the following endpoint contracts:
- `GET /api/v1/inventory/**`
- `GET/PUT /api/v1/profile/**`
- `POST/GET /api/v1/purchases/**`

---

## Session Cookie Properties

```
Set-Cookie: SESSION=<uuid>; 
            HttpOnly; 
            Secure; 
            SameSite=Lax; 
            Path=/; 
            Max-Age=<session-timeout-seconds>
```

Configured via `application.properties`:
```properties
server.servlet.session.cookie.name=SESSION
server.servlet.session.cookie.http-only=true
server.servlet.session.cookie.secure=true
server.servlet.session.cookie.same-site=lax
server.servlet.session.timeout=1800
```
