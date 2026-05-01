# Feature Specification: PKCE Auth Migration

**Feature Branch**: `002-pkce-auth-migration`  
**Created**: 2025-07-15  
**Status**: Draft  

## Overview

Migrate the BFF authentication from the insecure Resource Owner Password Credentials (ROPC) grant to Authorization Code + PKCE combined with the BFF-for-Security (Token Handler) pattern. After this migration, the BFF will never handle or store user credentials; Keycloak owns the credential exchange entirely. The frontend receives only an opaque HttpOnly session cookie, and all token lifecycle management (storage, refresh, injection) happens server-side within the BFF.

This migration closes the primary security gap in the current design — plaintext passwords flowing through the BFF — and eliminates the insecure practice of exposing access tokens to browser-side JavaScript.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Login via Keycloak Redirect (Priority: P1)

A user who wants to access the dealership application initiates login from the frontend. Instead of submitting an email and password form directly to the BFF, the user is redirected to the Keycloak-hosted login page (customised with Keycloakify). After successfully authenticating with Keycloak, the user is redirected back to the BFF's callback endpoint. The BFF completes the PKCE code exchange, stores tokens server-side in a distributed session, and returns an opaque HttpOnly session cookie to the browser. The frontend never receives or handles any token.

**Why this priority**: This is the foundational capability that replaces the insecure ROPC flow. Without it, nothing else in this migration can be tested or delivered. It eliminates credential exposure at the BFF level.

**Independent Test**: Can be fully tested by opening the application in a browser, clicking login, completing credentials on the Keycloak page, and verifying that the browser receives a session cookie — not a token — while being redirected to the authenticated area of the application.

**Acceptance Scenarios**:

1. **Given** an unauthenticated user visits the application login page, **When** they click "Login", **Then** their browser is redirected to the Keycloak-hosted login page with a valid PKCE `code_challenge` and `state` parameter in the URL.
2. **Given** the user enters valid credentials on the Keycloak login page, **When** Keycloak processes the authentication, **Then** the browser is redirected to the BFF callback endpoint with an authorization `code` and matching `state`.
3. **Given** the BFF callback endpoint receives a valid authorization `code`, **When** the code exchange completes successfully, **Then** the BFF sets an HttpOnly, Secure, SameSite=Lax session cookie on the browser response and redirects the user to the authenticated application area.
4. **Given** the code exchange completes, **When** inspecting the browser's storage (cookies, localStorage, sessionStorage), **Then** no access token or refresh token is visible to JavaScript — only the opaque session cookie is present.
5. **Given** an invalid or expired `state` parameter arrives at the callback endpoint, **When** the BFF validates the state, **Then** the request is rejected with an appropriate error and the user is redirected to the login page.
6. **Given** a user submits invalid credentials on the Keycloak page, **When** Keycloak rejects the authentication, **Then** the user sees an error on the Keycloak login page and is not redirected to the BFF callback.
7. **Given** a user attempts to replay a previously used authorization code, **When** the BFF attempts the code exchange, **Then** Keycloak rejects it and the user is redirected to the login page.

---

### User Story 2 — Session-Based API Access (Priority: P2)

An authenticated user performs actions in the application (browse inventory, view a deal, submit an offer). The frontend sends requests to the BFF using only the session cookie — it has no access token. The BFF looks up the session, retrieves the stored access token, checks expiry, refreshes it silently if needed, and forwards the request to downstream APIs (car-api, sales-api) with the correct `Authorization: Bearer` header injected. The frontend is entirely unaware of token lifetimes or refresh events.

**Why this priority**: This is the core security benefit of the BFF-for-Security pattern. Without it, session cookies are useless — they must translate into authenticated downstream calls. This story also replaces the existing `TokenRefreshFilter` with a session-aware equivalent.

**Independent Test**: Can be fully tested by logging in (US1 complete), then making any authenticated API call using only the session cookie and verifying that the downstream response is returned correctly, including after the access token has expired.

**Acceptance Scenarios**:

1. **Given** a user has a valid session cookie, **When** they make any authenticated request to the BFF, **Then** the BFF translates the session into an authenticated downstream call without requiring any token in the request.
2. **Given** a user's access token stored in the session has expired but the refresh token is still valid, **When** the user makes an authenticated request, **Then** the BFF silently refreshes the access token, stores the new token in the session, and fulfils the original request without interrupting the user.
3. **Given** a user sends a request without a session cookie (or with an invalid/expired session), **When** the BFF processes the request, **Then** the response is HTTP 401 and the frontend is directed to re-authenticate.
4. **Given** a user's refresh token has also expired, **When** the user makes an authenticated request, **Then** the session is invalidated, the session cookie is cleared, and the user receives HTTP 401 directing them to re-authenticate.
5. **Given** a valid authenticated session, **When** the user makes concurrent requests, **Then** all requests are fulfilled correctly without race conditions or duplicate refresh attempts.

---

### User Story 3 — Secure Logout (Priority: P3)

An authenticated user logs out of the application. The BFF invalidates the server-side session (removing stored tokens from the distributed session store), clears the session cookie from the browser, and initiates an OIDC logout request to Keycloak's `end_session` endpoint. After logout, the user's Keycloak SSO session is also terminated so they cannot re-enter the application without re-authenticating, and any previously issued tokens are revoked at the identity provider level.

**Why this priority**: Logout is a security-critical feature, but the application delivers no user value without login (P1) and session-based access (P2) first. Incomplete logout leaves tokens orphaned in Keycloak but the session-based architecture limits the blast radius since tokens are not browser-accessible.

**Independent Test**: Can be fully tested by logging in, performing logout, verifying the session cookie is cleared, verifying that a subsequent request with the old session cookie returns 401, and verifying that the Keycloak session is also terminated (no active session visible in Keycloak admin or via introspection).

**Acceptance Scenarios**:

1. **Given** a user with a valid session initiates logout, **When** the BFF processes the logout request, **Then** the server-side session is immediately invalidated and the session cookie is cleared from the browser.
2. **Given** logout is triggered, **When** the BFF completes session invalidation, **Then** a redirect to Keycloak's OIDC `end_session` endpoint is issued, passing the `id_token_hint` and a `post_logout_redirect_uri`.
3. **Given** Keycloak confirms session termination, **When** the post-logout redirect arrives, **Then** the user arrives at the application's logged-out page or login page.
4. **Given** a user has logged out and attempts to reuse the old session cookie, **When** the BFF receives the request, **Then** it returns HTTP 401 — the session is no longer valid.
5. **Given** a user has logged out, **When** they attempt to navigate to a protected BFF route directly, **Then** they are redirected to the Keycloak login page rather than returning cached content.

---

### User Story 4 — Registration Unchanged (Priority: P4)

Existing user registration via `POST /api/v1/auth/register` continues to function exactly as today, using the Keycloak Admin API to create users. No changes are made to the registration endpoint or its underlying service logic in this migration phase.

**Why this priority**: Registration is explicitly out of scope for this migration. It is listed to clearly bound what is NOT changing, preventing accidental scope creep.

**Independent Test**: Can be verified by calling `POST /api/v1/auth/register` with valid user data and confirming the user is created in Keycloak exactly as before the migration.

**Acceptance Scenarios**:

1. **Given** a request to `POST /api/v1/auth/register` with valid user data, **When** the BFF processes the request, **Then** the user is created in Keycloak via the Admin API exactly as it operates today — no behaviour change.
2. **Given** the PKCE login flow is fully implemented, **When** a user registers and then attempts to log in, **Then** the new PKCE login flow handles authentication for the newly registered user without issue.

---

### Edge Cases

- What happens when the Keycloak server is unreachable during the code exchange callback? The user should receive a clear error and be redirected to the login page; existing resilience patterns (Resilience4j circuit breaker, retry) should apply where applicable.
- What happens when the distributed session store (Redis) is unavailable? Authenticated requests should fail gracefully with 503 rather than silently bypassing authentication.
- What happens when a user opens multiple browser tabs and logs out in one? All sessions associated with the same Keycloak SSO session should be invalidated consistently.
- What happens when the `post_logout_redirect_uri` is not registered with Keycloak? The OIDC logout should still proceed; the user may land on Keycloak's default post-logout page.
- What happens when a state parameter mismatch is detected at the callback? The request must be rejected immediately to prevent CSRF attacks; no code exchange should proceed.
- What happens during a token refresh if Keycloak returns an error (e.g., refresh token revoked)? The session must be invalidated and the user directed to re-authenticate.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The BFF MUST expose a login initiation endpoint that generates a PKCE `code_verifier` / `code_challenge` pair, stores the verifier server-side, and redirects the browser to Keycloak's authorisation endpoint with `response_type=code`, `code_challenge`, `code_challenge_method=S256`, and a cryptographically random `state` parameter.
- **FR-002**: The BFF MUST expose a callback endpoint that validates the `state` parameter, completes the PKCE authorisation code exchange with Keycloak, and stores the resulting access token and refresh token exclusively in a server-side distributed session — never in the HTTP response body or browser-accessible storage.
- **FR-003**: After a successful code exchange, the BFF MUST set an HttpOnly, Secure, SameSite session cookie on the browser and redirect the user to the post-login destination.
- **FR-004**: The BFF MUST, on every authenticated request, look up the session associated with the incoming session cookie and inject the stored access token as an `Authorization: Bearer` header for all downstream API calls.
- **FR-005**: The BFF MUST automatically refresh an expired access token using the stored refresh token without interrupting the user's request, and MUST update the session with the new tokens immediately after refresh.
- **FR-006**: The BFF MUST expose a logout endpoint that invalidates the server-side session, clears the session cookie, and redirects to Keycloak's OIDC `end_session` endpoint with `id_token_hint` and `post_logout_redirect_uri`.
- **FR-007**: The BFF MUST reject any request to a protected resource that has no valid session cookie, or whose session is expired or invalid, with HTTP 401.
- **FR-008**: The `POST /api/v1/auth/register` endpoint MUST remain unchanged — it continues to use the Keycloak Admin API to create users, and its behaviour must be identical before and after the migration.
- **FR-009**: The BFF MUST maintain the existing resilience patterns (circuit breaker, retry) for all Keycloak interactions where those patterns currently apply.
- **FR-010**: The BFF MUST NOT log, persist, or expose plaintext user credentials at any point during the login flow.
- **FR-011**: The BFF MUST prevent CSRF attacks on the login callback by validating the `state` parameter against the server-side stored value before processing any authorisation code.

### Key Entities

- **Session**: Represents an authenticated user's server-side state. Contains the access token, refresh token, token expiry metadata, and user identity claims. Stored in the distributed session store. Identified by an opaque session ID referenced by the browser cookie.
- **PKCE Code Verifier**: A cryptographically random string generated per-login-attempt, stored server-side for the duration of the authorisation flow, and used to prove the initiator of the code exchange matches the initiator of the login redirect.
- **State Parameter**: A cryptographically random string tied to a login initiation request, validated at the callback to prevent CSRF. Stored server-side alongside the code verifier and discarded after validation.
- **Authorisation Code**: A short-lived, single-use code issued by Keycloak after successful user authentication. Exchanged by the BFF for access and refresh tokens; never exposed to the browser.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The BFF never receives, logs, or stores a user's plaintext password during the login flow — verifiable by code audit and runtime inspection of all log output and session data during a login attempt.
- **SC-002**: No access token or refresh token is present in any browser-accessible storage (localStorage, sessionStorage, non-HttpOnly cookies, or response bodies) after a successful login — verifiable by browser developer tools inspection post-login.
- **SC-003**: A user with an expired access token but valid refresh token experiences no interruption or visible delay greater than that of a normal authenticated request — transparent refresh completes within the existing request latency budget.
- **SC-004**: 100% of previously authenticated API call flows continue to function correctly after the migration, as verified by existing integration or end-to-end tests passing without modification to test scenarios.
- **SC-005**: Logout terminates both the BFF session and the Keycloak SSO session, confirmed by a subsequent direct API call with the old session cookie returning HTTP 401 and Keycloak's session introspection showing no active session.
- **SC-006**: The `POST /api/v1/auth/register` endpoint behaves identically before and after the migration — zero regression in registration behaviour.
- **SC-007**: The CSRF protection (state parameter validation) rejects 100% of callback requests with an invalid or missing state, with no code exchange attempted.

---

## Assumptions

- Keycloak is already configured and reachable; this migration assumes the Keycloak realm, client, and redirect URIs will be updated to support the `authorization_code` grant with PKCE (disabling the `password` grant for the BFF client in Keycloak is a deployment-time step, not a code deliverable).
- The distributed session store (Redis via Spring Session) is already available in the infrastructure; this feature assumes the Redis instance used by other services (e.g., Elasticache) can be reused or a dedicated instance provisioned — session storage capacity planning is out of scope for this spec.
- The frontend application will be updated in a parallel effort to remove the `Authorization: Bearer` header logic and rely solely on the session cookie; the BFF migration and frontend migration will be coordinated for deployment.
- The Keycloakify-customised login page is already available or will be delivered by a separate effort; the PKCE flow redirects to whatever login page Keycloak presents — the BFF does not own the login UI.
- The existing BFF client component used for Keycloak Admin API operations (register, createUser, deleteUser) remains in use and is not affected by this migration.
- `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`, and the existing `TokenRefreshFilter` will be removed or disabled as part of this migration — backward compatibility with the ROPC flow is not required after cutover.
- The CORS and cookie security settings (Secure flag, SameSite policy) will be appropriately configured for the deployment environment; local development may use relaxed settings.
- Standard OIDC session timeout and token TTL values configured in Keycloak are acceptable defaults; fine-tuning TTLs is out of scope for this spec.
