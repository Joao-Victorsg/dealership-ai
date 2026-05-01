# Research: PKCE Auth Migration

**Feature**: `002-pkce-auth-migration`  
**Phase**: Phase 0 — Research  
**Date**: 2025-07-15

---

## 1. Spring OAuth2 Client — PKCE Mechanics

**Decision**: Use `spring-boot-starter-oauth2-client` as the sole PKCE implementation.

**Rationale**: Spring Security 7.x's `OAuth2LoginConfigurer` natively generates a random `code_verifier`, derives the S256 `code_challenge`, stores both server-side (in the session), appends them to the authorization URL, and validates the callback's `state` parameter — all with zero application boilerplate. Writing these cryptographic primitives manually would duplicate battle-tested framework code and introduce risk.

**Alternatives considered**:
- Manual PKCE implementation (custom filter generating code_verifier, storing in Redis, handling callback) — rejected: unnecessary complexity, re-invents what Spring already provides.
- Nimbus OAuth2 SDK directly — rejected: lower-level, no Spring Security integration, more wiring required.

**Spring configuration surface**:
```properties
spring.security.oauth2.client.registration.keycloak.client-id=dealership-bff
spring.security.oauth2.client.registration.keycloak.authorization-grant-type=authorization_code
spring.security.oauth2.client.registration.keycloak.scope=openid,profile,email
spring.security.oauth2.client.provider.keycloak.issuer-uri=...
```
Spring derives the authorization, token, and end_session endpoints from the OIDC discovery document at `issuer-uri/.well-known/openid-configuration`.

---

## 2. Spring Session Data Redis — Distributed Session Storage

**Decision**: Add `spring-session-data-redis` and configure `spring.session.store-type=redis`.

**Rationale**: `spring-boot-starter-data-redis` is already present in `pom.xml`. Spring Session auto-configures on top of the existing `RedisConnectionFactory` bean declared in `RedisConfig.java`. No second Redis connection pool is created. Spring Session replaces the default in-memory `HttpSession` with a Redis-backed one, making it cluster-safe and persistent across BFF restarts.

**Key behaviour**:
- Session cookie name: `SESSION` (HttpOnly, Secure, SameSite=Lax by default; configurable via `server.servlet.session.cookie.*`)
- Session timeout: driven by `server.servlet.session.timeout` (default 30 min); should be aligned with Keycloak's SSO session timeout.
- Spring Session serialises session attributes using Jackson (default in Boot 4.x when `jackson-databind` is on classpath). Token strings are plain `String` — no special serialisation needed.

**Alternatives considered**:
- In-memory session (default) — rejected: not cluster-safe, lost on restart.
- JDBC session — rejected: adds a database dependency for session data with no latency advantage.

---

## 3. OAuth2AuthorizedClientManager — Transparent Token Refresh

**Decision**: Use `OAuth2AuthorizedClientManager` (specifically `DefaultOAuth2AuthorizedClientManager`) injected into `SessionTokenInjectionFilter` to perform transparent refresh.

**Rationale**: `DefaultOAuth2AuthorizedClientManager` is the canonical Spring component for managing the lifecycle of an `OAuth2AuthorizedClient`, including token refresh via `RefreshTokenOAuth2AuthorizedClientProvider`. It reads from and writes back to `OAuth2AuthorizedClientRepository`, which when combined with Spring Session persists the refreshed token automatically. The filter only needs to call `authorizedClientManager.authorize(request)` — refresh happens transparently if the access token is expired.

**Concurrency note**: Spring Session's Redis backend serialises session attribute writes. If two concurrent requests arrive with an expired token, both may attempt refresh; the second will succeed if the first has already updated the session, or will do a redundant refresh. This is acceptable behaviour (two refreshes race; the last writer wins; no session is lost).

**Alternatives considered**:
- Manual expiry check + `KeycloakClient.login(refreshForm)` — rejected: bypasses Spring's token lifecycle management, duplicates logic, misses edge cases like concurrent refresh.
- `WebClient` with `ServerOAuth2AuthorizedClientExchangeFilterFunction` — rejected: reactive, incompatible with servlet-based BFF.

---

## 4. OIDC Logout — OidcClientInitiatedLogoutSuccessHandler

**Decision**: Use `OidcClientInitiatedLogoutSuccessHandler` for OIDC end-session logout.

**Rationale**: Spring Security 7.x provides this handler out-of-the-box. It reads `end_session_endpoint` from Keycloak's OIDC discovery document, appends `id_token_hint` (taken from the session's `OidcIdToken`) and `post_logout_redirect_uri`, and issues the redirect. This ensures Keycloak's SSO session is terminated in addition to the BFF session.

**Configuration**:
```java
.logout(logout -> logout
    .logoutSuccessHandler(oidcLogoutSuccessHandler())
    .deleteCookies("SESSION")
    .invalidateHttpSession(true))
```
The `post_logout_redirect_uri` must be registered in Keycloak's client configuration.

**Alternatives considered**:
- Calling Keycloak `/logout` endpoint via Feign manually in `AuthService` — rejected: still works but loses the ID token hint and doesn't terminate the Keycloak browser session properly for OIDC; also requires manual URI construction.

---

## 5. Session Attribute Schema

**Decision**: Store token data in `HttpSession` using the following attribute keys after successful PKCE code exchange:

| Key | Value | Set by |
|-----|-------|--------|
| `bff.access_token` | JWT access token string | `OAuth2LoginSuccessHandler` |
| `bff.refresh_token` | Opaque refresh token string | `OAuth2LoginSuccessHandler` |
| `bff.id_token` | OIDC ID token string | `OAuth2LoginSuccessHandler` |
| `bff.token_expiry` | `Instant` of access token expiry | `OAuth2LoginSuccessHandler` / `SessionTokenInjectionFilter` on refresh |

**Rationale**: Naming with `bff.` prefix avoids collisions with Spring's own session attributes (e.g., `SPRING_SECURITY_CONTEXT`). Storing expiry separately allows the filter to do a cheap Instant comparison before invoking `OAuth2AuthorizedClientManager`.

**Note**: `OAuth2AuthorizedClientManager` also stores the `OAuth2AuthorizedClient` object in the session (via `HttpSessionOAuth2AuthorizedClientRepository`). The filter can use either path. Using the explicit `bff.access_token` attribute is preferred for clarity and to avoid dependency on Spring's internal serialisation of `OAuth2AuthorizedClient`.

---

## 6. Security Config Changes — SessionCreationPolicy and CSRF

**Decision**: Change `SessionCreationPolicy` from `STATELESS` to `IF_REQUIRED` and enable Spring Security's default CSRF protection for state-mutating requests (or rely on SameSite=Strict cookie policy).

**Rationale**: `SessionCreationPolicy.STATELESS` prevents session creation. `IF_REQUIRED` allows Spring to create and use sessions, which is required for OAuth2 login (state storage), Spring Session, and authenticated requests. CSRF: the oauth2Login flow uses the `state` parameter as its own CSRF guard. For other POST endpoints (`/api/v1/auth/logout`, `/api/v1/auth/register`), SameSite=Strict (or Lax) on the session cookie provides CSRF protection without a separate synchroniser token.

**Alternatives considered**:
- Keeping STATELESS and using a custom session store — rejected: incompatible with Spring Session and oauth2Login which rely on `HttpSession`.

---

## 7. pom.xml Dependencies

**Decision**: Add exactly two dependencies.

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.session</groupId>
    <artifactId>spring-session-data-redis</artifactId>
</dependency>
```

**Rationale**: `spring-boot-starter-oauth2-client` pulls in `spring-security-oauth2-client` and `spring-security-oauth2-jose`. `spring-session-data-redis` is not in the Spring Cloud BOM; it is managed by the Spring Boot parent BOM at the version compatible with Boot 4.0.6.

**Note on resilience4j**: The `AuthService.register()` method retains all five resilience annotations (`@CircuitBreaker`, `@Retry`, `@RateLimiter`, `@TimeLimiter`, `@Bulkhead` — all named `keycloak`). The new `AuthService.logout()` should also carry the `keycloak` circuit breaker annotation for the Feign call to Keycloak's revocation endpoint, if any is made. The `OAuth2LoginSuccessHandler` and `SessionTokenInjectionFilter` do not make direct Feign calls — Spring's `OAuth2AuthorizedClientManager` uses its own HTTP client for token refresh, which is separate from the Feign stack.

---

## 8. KeycloakClient Feign — Remaining Methods After Migration

**Decision**: Retain `KeycloakClient` but remove the `login()` call path from non-register flows. The `login(form)` method used for ROPC and refresh token grant is no longer called directly. The `logout(form)` method is no longer needed since OIDC logout is handled by Spring.

**Methods retained** (called by `AuthService.register()`):
- `createUser(token, request)`
- `searchUsers(token, email, exact)`
- `sendVerifyEmail(token, userId)`
- `deleteUser(token, userId)`
- `login(form)` — kept for `obtainAdminToken()` (client_credentials grant for Admin API)

**Methods no longer called**:
- `login(form)` for password grant (ROPC) — `AuthService.login()` deleted
- `logout(form)` — `AuthService.logout()` updated to not call Keycloak logout via Feign; OIDC end_session is handled by Spring

**Note**: `KeycloakClient` interface itself does not need to change; unused methods are harmless.
