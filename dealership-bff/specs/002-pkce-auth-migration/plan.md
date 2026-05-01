# Implementation Plan: PKCE Auth Migration

**Branch**: `002-pkce-auth-migration` | **Date**: 2025-07-15 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `specs/002-pkce-auth-migration/spec.md`

---

## Summary

Migrate the BFF from ROPC (`grant_type=password`) to Authorization Code + PKCE
combined with the BFF-for-Security (Token Handler) pattern. Spring's
`spring-boot-starter-oauth2-client` handles all PKCE mechanics (code_verifier,
code_challenge, state, code exchange) with zero boilerplate.
`spring-session-data-redis` stores the HttpSession — including access and
refresh tokens — in Redis. Two custom classes bridge Spring's OAuth2 client
machinery with the existing resource-server filter chain:
`OAuth2LoginSuccessHandler` (stores token claims in session post-login) and
`SessionTokenInjectionFilter` (reads session, injects `Authorization: Bearer`
for downstream Feign calls, handles transparent refresh via
`OAuth2AuthorizedClientManager`). After migration, the frontend receives only
an opaque HttpOnly `SESSION` cookie; tokens never reach the browser.

---

## Technical Context

**Language/Version**: Java 25  
**Primary Dependencies**: Spring Boot 4.0.6, Spring Security 7.x,
  spring-boot-starter-oauth2-client (new), spring-session-data-redis (new),
  spring-boot-starter-oauth2-resource-server (existing),
  spring-cloud-starter-openfeign, resilience4j-spring-boot3 2.3.0  
**Storage**: Redis (existing ElastiCache connection reused by Spring Session)  
**Testing**: JUnit 5, Mockito, Instancio 5.3.0, REST Assured 6.0.0,
  Testcontainers, WireMock (wiremock-spring-boot 3.6.0), JaCoCo 0.8.13,
  PITest 1.19.1  
**Target Platform**: Linux server (Docker / ECS)  
**Project Type**: Web service (BFF — Backend for Frontend)  
**Performance Goals**: p99 ≤ 500 ms, p50 ≤ 300 ms (Article V)  
**Constraints**: Tokens must never reach the browser (SC-002). All Keycloak
  outbound calls protected by Resilience4j chain (Article VI).  
**Scale/Scope**: Single-module Spring Boot service; migration affects ~7 source
  files; 2 new files; 3 deletions.

---

## Constitution Check

*GATE: Must pass before implementation. Re-check after Phase 1 design.*

### Article II — Library Versions ✅ PASS

Spring Boot 4.0.6 is the version in use. `spring-session-data-redis` and
`spring-boot-starter-oauth2-client` are managed by the Spring Boot parent BOM —
no manual version declaration required.

**Residual note**: `pom.xml` uses `resilience4j-spring-boot3` artifact ID
(`version 2.3.0`). The constitution specifies `resilience4j-spring-boot4` latest
stable. This pre-existing mismatch is out of scope for this feature but should
be addressed as a follow-up amendment.

### Article III — Response Envelope Contract ✅ PASS

`POST /api/v1/auth/logout` and `POST /api/v1/auth/register` continue to return
the `ApiResponse<T>` envelope. The removed `/login` and `/refresh` endpoints no
longer need envelopes. `OAuth2LoginSuccessHandler` issues an HTTP redirect (no
response body), which is the correct pattern for the browser-facing PKCE
callback — not a JSON response.

### Article IV — Security ✅ PASS (improved)

This migration directly addresses the primary security gap: plaintext passwords
no longer flow through the BFF. JWT tokens no longer reach the browser. PKCE
S256 prevents authorization code interception. State parameter prevents CSRF
during the auth flow. `SameSite=Lax` session cookie prevents most CSRF on API
calls. Keycloak OIDC end_session properly revokes the SSO session on logout.

The `SessionTokenInjectionFilter` must strip the injected `Authorization:
Bearer` header from the original client request (if present — the client should
no longer send it), and inject the session-derived token. This prevents
client-supplied token spoofing.

### Article V — Performance and Parallelism ✅ PASS

Token injection in `SessionTokenInjectionFilter` is an in-process session
lookup (Redis call ≤ 1 ms in-cluster). If token refresh is needed,
`OAuth2AuthorizedClientManager` issues one HTTP call to Keycloak before the
downstream request proceeds. This is equivalent to the current
`TokenRefreshFilter` latency profile.

### Article VI — Resilience ✅ PASS

`AuthService.register()` retains all five Resilience4j annotations on the
`keycloak` named instance. `AuthService.logout()` (updated) calls
`session.invalidate()` and relies on Spring's redirect to Keycloak — no direct
Feign call for the end_session. The `OAuth2AuthorizedClientManager` token
refresh uses Spring's internal HTTP client (not Feign) — **this path is not
covered by Resilience4j**. This is an accepted limitation: if Keycloak is
unavailable during a token refresh, the manager throws an exception which
`SessionTokenInjectionFilter` must catch and translate to a 401, matching the
existing TokenRefreshFilter behaviour.

**Violation**: `OAuth2AuthorizedClientManager`'s internal HTTP client bypasses
the Feign Resilience4j stack. See Complexity Tracking below.

### Article VII — Immutability by Design ✅ PASS

`OAuth2LoginSuccessHandler` and `SessionTokenInjectionFilter` are new classes.
All fields must be `final`. Parameters in all methods must be `final`. No new
mutable DTOs are introduced; `LoginRequest` and `TokenResponse` are deleted.

### Article IX — Testing Requirements ✅ PASS

New classes `OAuth2LoginSuccessHandler` and `SessionTokenInjectionFilter` must
achieve ≥90% line and branch coverage via unit tests (Mockito). Integration
tests must cover: successful login redirect flow (WireMock Keycloak stubs),
session-based API call, transparent token refresh, session expiry → 401, and
logout flow. PITest must exclude the new `config/` classes from mutation scope
(consistent with existing JaCoCo exclusions).

### Article X — Observability ✅ PASS

`SessionTokenInjectionFilter` must log (at WARN) when token refresh fails,
including `requestId` from MDC, consistent with `TokenRefreshFilter`'s existing
pattern. The injected access token must never appear in log output.

### Article XI — API Design Principles ✅ PASS

`/oauth2/authorization/keycloak` and `/login/oauth2/code/keycloak` are
Spring-managed and follow OIDC standards. They do not need OpenAPI documentation.
Updated `/logout` and unchanged `/register` retain existing OpenAPI annotations.

---

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|--------------------------------------|
| `OAuth2AuthorizedClientManager` token refresh bypasses Resilience4j Feign stack | Spring's OAuth2 client uses its own `RestClient`/`RestTemplate`; wrapping it with Resilience4j annotations would require custom `OAuth2AccessTokenResponseClient` implementation — significant complexity | Catch-and-401 on refresh failure is equivalent to the existing TokenRefreshFilter behaviour; the Resilience4j circuit breaker on `register()` is retained for Admin API calls. Adding Resilience4j to token refresh is deferred to a future hardening feature. |

---

## Project Structure

### Documentation (this feature)

```text
specs/002-pkce-auth-migration/
├── spec.md              ✅ Written
├── plan.md              ✅ This file
├── research.md          ✅ Phase 0 complete
├── data-model.md        ✅ Phase 1 complete
├── quickstart.md        ✅ Phase 1 complete
├── contracts/
│   └── auth-flow.md     ✅ Phase 1 complete
└── tasks.md             🔲 Phase 2 (generated by /speckit.tasks)
```

### Source Code Changes

```text
dealership-bff/
├── pom.xml                                          MODIFY — add 2 dependencies
├── src/main/
│   ├── resources/
│   │   └── application.properties                  MODIFY — add OAuth2 + Session config
│   └── java/br/com/dealership/dealershibff/
│       ├── config/
│       │   ├── SecurityConfig.java                 MODIFY — oauth2Login, logout, filter swap
│       │   ├── OAuth2LoginSuccessHandler.java       CREATE — stores tokens in session
│       │   └── RedisConfig.java                    NO CHANGE
│       ├── web/
│       │   ├── SessionTokenInjectionFilter.java     CREATE — replaces TokenRefreshFilter
│       │   └── TokenRefreshFilter.java              DELETE
│       ├── service/
│       │   └── AuthService.java                    MODIFY — remove login/refresh/writeRefreshCookie/clearRefreshCookie
│       ├── controller/
│       │   └── AuthController.java                 MODIFY — remove login/refresh endpoints
│       └── dto/
│           ├── request/
│           │   └── LoginRequest.java               DELETE
│           └── response/
│               └── TokenResponse.java              DELETE
└── src/test/
    └── java/br/com/dealership/dealershibff/
        ├── config/
        │   └── OAuth2LoginSuccessHandlerTest.java  CREATE — unit tests
        ├── web/
        │   └── SessionTokenInjectionFilterTest.java CREATE — unit tests
        ├── service/
        │   └── AuthServiceTest.java                MODIFY — remove login/refresh test cases
        ├── controller/
        │   └── AuthControllerTest.java             MODIFY — remove login/refresh test cases
        └── integrated/
            └── auth/
                └── PkceAuthFlowIT.java             CREATE — integration tests
```

**Structure Decision**: Single Spring Boot module, unchanged. No new packages
introduced. New classes are placed in existing `config/` and `web/` packages
consistent with current conventions.

---

## Phase 0: Research Summary

All technical unknowns resolved. See [research.md](./research.md) for full
decisions. Key outcomes:

1. `spring-boot-starter-oauth2-client` handles PKCE natively — zero application-level PKCE boilerplate.
2. `spring-session-data-redis` reuses the existing `RedisConnectionFactory` — no second Redis connection pool.
3. `DefaultOAuth2AuthorizedClientManager` performs transparent token refresh — filter delegates refresh to Spring.
4. `OidcClientInitiatedLogoutSuccessHandler` handles OIDC end_session logout with `id_token_hint`.
5. Session attributes `bff.access_token`, `bff.refresh_token`, `bff.id_token`, `bff.token_expiry` store token data.
6. `SessionCreationPolicy` changes from `STATELESS` → `IF_REQUIRED`.
7. `KeycloakClient` Feign interface is retained (needed for `obtainAdminToken()` in `register()`).

---

## Phase 1: Design Decisions

See [data-model.md](./data-model.md), [contracts/auth-flow.md](./contracts/auth-flow.md), and [quickstart.md](./quickstart.md).

### Key Design Decisions

#### `OAuth2LoginSuccessHandler`

- Implements `AuthenticationSuccessHandler`
- Injected into `SecurityConfig` as a `@Bean`
- Receives `OAuth2AuthenticationToken` (contains `OAuth2AuthorizedClient` and claims)
- Reads `OAuth2AuthorizedClient` via `OAuth2AuthorizedClientService.loadAuthorizedClient()`
- Writes `bff.access_token`, `bff.refresh_token`, `bff.id_token`, `bff.token_expiry` to `HttpSession`
- Redirects browser to `${app.post-login-redirect-uri}`
- Fields: `final OAuth2AuthorizedClientService clientService`, `final String postLoginRedirectUri`

#### `SessionTokenInjectionFilter`

- Extends `OncePerRequestFilter`
- Replaces `TokenRefreshFilter` in `SecurityConfig` (same insertion point: before `BearerTokenAuthenticationFilter`)
- Algorithm:
  1. Read `bff.access_token` from `HttpSession`; if null → pass through (unauthenticated request, let Spring Security handle 401)
  2. Compare `bff.token_expiry` with `Instant.now()`; if not expired → inject header directly
  3. If expired → call `OAuth2AuthorizedClientManager.authorize()` → on success, update `bff.*` session attributes and inject new token
  4. On refresh failure → log WARN with requestId, invalidate session, clear SESSION cookie, pass through (Spring Security returns 401)
  5. Inject `Authorization: Bearer <token>` using a `HttpServletRequestWrapper`
- Fields: `final OAuth2AuthorizedClientManager authorizedClientManager`

#### `SecurityConfig` changes

```java
// REMOVE:
.addFilterBefore(new TokenRefreshFilter(authService), BearerTokenAuthenticationFilter.class)
.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
.requestMatchers(POST, "/api/v1/auth/login").permitAll()
.requestMatchers(POST, "/api/v1/auth/refresh").permitAll()

// ADD:
.addFilterBefore(sessionTokenInjectionFilter, BearerTokenAuthenticationFilter.class)
.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
.oauth2Login(oauth2 -> oauth2
    .successHandler(oauth2LoginSuccessHandler))
.logout(logout -> logout
    .logoutUrl("/api/v1/auth/logout")
    .logoutSuccessHandler(oidcLogoutSuccessHandler())
    .deleteCookies("SESSION")
    .invalidateHttpSession(true))
// Spring auto-permits /oauth2/authorization/** and /login/oauth2/code/**
```

#### `AuthService` retained method

`register()` is unchanged. `logout()` is removed from `AuthService` (handled by Spring Security's logout configurer and `OidcClientInitiatedLogoutSuccessHandler`). The `AuthController.logout()` endpoint is removed — `SecurityConfig` registers `/api/v1/auth/logout` as the logout URL.

**Note on logout API response**: Spring Security's `LogoutFilter` does not produce a JSON envelope response. If the frontend calls `POST /api/v1/auth/logout` as a REST call (not a browser form), the response will be a `302 redirect` to Keycloak's end_session endpoint. The frontend must follow this redirect. If a JSON `204` response is required, a custom `LogoutSuccessHandler` that writes the envelope and then redirects must be implemented.

#### `AuthController` after migration

Retains only `POST /register`. `POST /login`, `POST /refresh`, and `POST /logout` are all removed from the controller. Logout is managed by Spring Security's `LogoutFilter`.

---

## Post-Design Constitution Check

| Article | Status | Notes |
|---------|--------|-------|
| II — Versions | ✅ | BOM-managed dependencies |
| III — Envelope | ✅ | `/logout` handled by Spring (redirect); `/register` unchanged |
| IV — Security | ✅ IMPROVED | Passwords never touch BFF; tokens never reach browser |
| V — Performance | ✅ | Redis session lookup adds ~1 ms; equivalent to existing cookie lookup |
| VI — Resilience | ⚠️ NOTED VIOLATION | OAuth2AuthorizedClientManager refresh bypasses Resilience4j; documented in Complexity Tracking; acceptable tradeoff |
| VII — Immutability | ✅ | All new class fields final; records retained for DTOs |
| IX — Testing | ✅ | Unit + integration test plan covers all acceptance scenarios |
| X — Observability | ✅ | WARN log on refresh failure; token never logged |
| XI — API Design | ✅ | OpenAPI annotations retained on unchanged endpoints |

---

## Next Steps

Run `/speckit.tasks` to generate `tasks.md` with dependency-ordered
implementation tasks covering:

1. pom.xml dependency additions
2. application.properties additions
3. `OAuth2LoginSuccessHandler` — create + unit test
4. `SessionTokenInjectionFilter` — create + unit test
5. `SecurityConfig` — modify
6. `AuthService` — simplify (remove login/refresh/cookie methods)
7. `AuthController` — remove login/refresh/logout endpoints
8. Delete `TokenRefreshFilter`, `LoginRequest`, `TokenResponse`
9. `AuthControllerTest` / `AuthServiceTest` — update
10. `PkceAuthFlowIT` — integration tests
