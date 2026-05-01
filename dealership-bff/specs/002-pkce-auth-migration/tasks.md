# Tasks: PKCE Auth Migration

**Input**: Design documents from `specs/002-pkce-auth-migration/`
**Prerequisites**: plan.md ✅, spec.md ✅, data-model.md ✅, contracts/auth-flow.md ✅, research.md ✅, quickstart.md ✅
**Tests**: Included — required by plan.md Article IX (≥90% coverage on new classes; integration suite mandatory)

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no shared-state dependencies)
- **[Story]**: Which user story this task belongs to (US1 → US4 mapped to spec.md priorities P1 → P4)
- Exact file paths are included in every task description

## Path Convention

```text
src/main/java/br/com/dealership/dealershibff/   ← production sources
src/test/java/br/com/dealership/dealershibff/    ← test sources
src/main/resources/                              ← application config
```

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Add the two new Spring Boot dependencies that gate everything else. Nothing else in this migration compiles until these are present in the POM.

- [ ] T001 Modify `pom.xml` — add `spring-boot-starter-oauth2-client` and `spring-session-data-redis` as managed dependencies under the Spring Boot 4.0.6 parent BOM (no explicit version declarations needed). Confirm `mvn dependency:resolve` succeeds before proceeding.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Configuration and new-class stubs that MUST exist before any SecurityConfig or test can compile.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [ ] T002 Modify `src/main/resources/application.properties` — add all OAuth2 client registration properties (`spring.security.oauth2.client.registration.keycloak.*`, `spring.security.oauth2.client.provider.keycloak.*`), Spring Session properties (`spring.session.store-type=redis`), session cookie properties (`server.servlet.session.cookie.name=SESSION`, `http-only=true`, `secure=true`, `same-site=lax`, `server.servlet.session.timeout=1800`), and app-level redirect URIs (`app.post-login-redirect-uri`, `app.post-logout-redirect-uri`). See contracts/auth-flow.md §Session Cookie Properties for full property list.

**Checkpoint**: Dependencies resolved, properties configured — class creation can now proceed in parallel.

---

## Phase 3: User Story 1 — Login via Keycloak Redirect (Priority: P1) 🎯 MVP

**Goal**: Replace the ROPC `/login` endpoint with a fully Spring-managed PKCE Authorization Code flow. After this phase, the browser initiates login at `/oauth2/authorization/keycloak`, Keycloak authenticates the user, and the BFF exchanges the code and writes an HttpOnly `SESSION` cookie. No password or token ever reaches the browser.

**Independent Test**: Open the app, click "Login", complete credentials on the Keycloak page, verify the browser receives a `SESSION` cookie (not a token) and is redirected to the post-login URL. Verify `localStorage`, `sessionStorage`, and non-HttpOnly cookies contain no token.

### Tests for User Story 1 ⚠️ Write FIRST — ensure they FAIL before implementing T004

- [ ] T003 [P] [US1] Write unit tests for `OAuth2LoginSuccessHandler` in `src/test/java/br/com/dealership/dealershibff/config/OAuth2LoginSuccessHandlerTest.java` — mock `OAuth2AuthorizedClientService`, `HttpSession`, and `OAuth2AuthenticationToken`; assert that `bff.access_token`, `bff.refresh_token`, `bff.id_token`, and `bff.token_expiry` are set on the session; assert redirect to `app.post-login-redirect-uri`; cover null/blank token edge cases. Tests must FAIL (class does not exist yet).

### Implementation for User Story 1

- [ ] T004 [P] [US1] Create `src/main/java/br/com/dealership/dealershibff/config/OAuth2LoginSuccessHandler.java` — implement `AuthenticationSuccessHandler`; inject `final OAuth2AuthorizedClientService clientService` and `final String postLoginRedirectUri` (`@Value("${app.post-login-redirect-uri}")`); in `onAuthenticationSuccess()`, read `OAuth2AuthorizedClient` via `clientService.loadAuthorizedClient()`, extract `accessToken`, `refreshToken`, `idToken`, and `exp` claim; write `bff.access_token`, `bff.refresh_token`, `bff.id_token`, `bff.token_expiry` to `HttpSession`; call `response.sendRedirect(postLoginRedirectUri)`. All fields must be `final` (Article VII).

- [ ] T005 [US1] Modify `src/main/java/br/com/dealership/dealershibff/config/SecurityConfig.java` — Part A: (a) switch `SessionCreationPolicy` from `STATELESS` to `IF_REQUIRED`; (b) add `.oauth2Login(oauth2 -> oauth2.successHandler(oauth2LoginSuccessHandler()))` where `oauth2LoginSuccessHandler()` is a `@Bean` returning `OAuth2LoginSuccessHandler`; (c) remove `permitAll` for `POST /api/v1/auth/login` and `POST /api/v1/auth/refresh`; (d) keep existing `permitAll` for `POST /api/v1/auth/register`. Spring auto-permits `/oauth2/authorization/**` and `/login/oauth2/code/**` — no explicit permit needed. Depends on T004.

- [ ] T006 [US1] Modify `src/main/java/br/com/dealership/dealershibff/controller/AuthController.java` — remove `POST /api/v1/auth/login` endpoint method and its `LoginRequest` parameter reference; remove `POST /api/v1/auth/refresh` endpoint method. Retain `POST /api/v1/auth/register` unchanged and `POST /api/v1/auth/logout` (will be removed in Phase 5 after SecurityConfig logout is wired). Depends on T005.

- [ ] T007 [US1] Delete `src/main/java/br/com/dealership/dealershibff/dto/request/LoginRequest.java` — no longer referenced after T006 removes the `/login` endpoint. Confirm `mvn compile` passes after deletion. Depends on T006.

**⚑ Checkpoint — US1 Complete**

> At this point the PKCE login flow is fully operational. Validate independently:
> 1. `GET /oauth2/authorization/keycloak` → 302 to Keycloak with `code_challenge`, `state` in URL.
> 2. Submit credentials on Keycloak → 302 back to BFF callback → `Set-Cookie: SESSION=...` in response.
> 3. Browser storage inspection shows no token (only `SESSION` cookie).
> 4. Replay of authorization code → Keycloak rejects it.
> 5. `OAuth2LoginSuccessHandlerTest` unit tests pass (≥90% coverage).

---

## Phase 4: User Story 2 — Session-Based API Access (Priority: P2)

**Goal**: Replace `TokenRefreshFilter` with `SessionTokenInjectionFilter`. After this phase, every authenticated request uses only the `SESSION` cookie — no `Authorization` header from the frontend. The filter reads the session, injects `Authorization: Bearer <token>`, and silently refreshes expired tokens via `OAuth2AuthorizedClientManager`.

**Independent Test**: Log in (US1 complete), then make an authenticated API call using only the session cookie (no `Authorization` header). Verify the downstream response is correct. Let the access token expire and verify the next request still succeeds (silent refresh). Remove the session cookie and verify HTTP 401.

### Tests for User Story 2 ⚠️ Write FIRST — ensure they FAIL before implementing T009

- [ ] T008 [P] [US2] Write unit tests for `SessionTokenInjectionFilter` in `src/test/java/br/com/dealership/dealershibff/web/SessionTokenInjectionFilterTest.java` — mock `HttpSession`, `OAuth2AuthorizedClientManager`, `HttpServletRequest`, `HttpServletResponse`, `FilterChain`; assert cases: (1) no session → filter passes through without injecting header; (2) valid non-expired token → `Authorization: Bearer` injected; (3) expired token + successful refresh → new token injected and session attributes updated; (4) expired token + refresh failure → WARN logged with `requestId` from MDC, session invalidated, `SESSION` cookie cleared, filter passes through (Spring Security returns 401); (5) client-supplied `Authorization` header stripped before injection. Tests must FAIL (class does not exist yet).

### Implementation for User Story 2

- [ ] T009 [P] [US2] Create `src/main/java/br/com/dealership/dealershibff/web/SessionTokenInjectionFilter.java` — extend `OncePerRequestFilter`; inject `final OAuth2AuthorizedClientManager authorizedClientManager`; implement algorithm: (1) read `bff.access_token` from `HttpSession`; if null → `chain.doFilter()` and return; (2) compare `bff.token_expiry` (`Instant`) with `Instant.now()`; if not expired → skip to step 5; (3) call `authorizedClientManager.authorize(OAuth2AuthorizeRequest...)` to refresh; on success → update `bff.*` session attributes with new token values; (4) on refresh failure → log WARN with `requestId` from MDC (token must NOT appear in log output — Article X), call `session.invalidate()`, add `SESSION` cookie with `Max-Age=0` to clear it, call `chain.doFilter()` and return; (5) inject `Authorization: Bearer <token>` by wrapping request in `HttpServletRequestWrapper` that overrides `getHeader("Authorization")`. All fields must be `final` (Article VII).

- [ ] T010 [US2] Modify `src/main/java/br/com/dealership/dealershibff/config/SecurityConfig.java` — Part B: (a) remove `.addFilterBefore(new TokenRefreshFilter(authService), BearerTokenAuthenticationFilter.class)`; (b) add `.addFilterBefore(sessionTokenInjectionFilter, BearerTokenAuthenticationFilter.class)` where `sessionTokenInjectionFilter` is a `@Bean` returning `SessionTokenInjectionFilter`; (c) declare `OAuth2AuthorizedClientManager` bean (using `DefaultOAuth2AuthorizedClientManager`) to be injected into the filter. Depends on T009.

- [ ] T011 [US2] Delete `src/main/java/br/com/dealership/dealershibff/web/TokenRefreshFilter.java` — no longer referenced in `SecurityConfig` after T010. Confirm `mvn compile` passes after deletion. Depends on T010.

**⚑ Checkpoint — US2 Complete**

> Session-based API access is now fully operational. Validate independently:
> 1. Authenticated `GET /api/v1/inventory` with only `SESSION` cookie → 200 from downstream.
> 2. Authenticated request with expired access token (manipulate `bff.token_expiry` in session or wait) → transparent refresh → 200.
> 3. Request with no `SESSION` cookie → 401.
> 4. Request with expired refresh token → session invalidated → 401.
> 5. `SessionTokenInjectionFilterTest` unit tests pass (≥90% coverage).

---

## Phase 5: User Story 3 — Secure Logout (Priority: P3)

**Goal**: Add OIDC-compliant logout. After this phase, `POST /api/v1/auth/logout` invalidates the server-side session, clears the `SESSION` cookie, and redirects to Keycloak's `end_session` endpoint with `id_token_hint`. The AuthController and AuthService are cleaned up.

**Independent Test**: Log in, then `POST /api/v1/auth/logout` → verify `SESSION` cookie is cleared. Make a subsequent request with the old session cookie → 401. Verify in Keycloak admin that the SSO session is terminated.

### Implementation for User Story 3

- [ ] T012 [US3] Modify `src/main/java/br/com/dealership/dealershibff/config/SecurityConfig.java` — Part C: add `.logout(logout -> logout.logoutUrl("/api/v1/auth/logout").logoutSuccessHandler(oidcLogoutSuccessHandler()).deleteCookies("SESSION").invalidateHttpSession(true))`; implement `oidcLogoutSuccessHandler()` `@Bean` returning `OidcClientInitiatedLogoutSuccessHandler` configured with `clientRegistrationRepository` and `app.post-logout-redirect-uri`. Depends on T005 and T010.

- [ ] T013 [US3] Modify `src/main/java/br/com/dealership/dealershibff/service/AuthService.java` — remove `login()`, `refresh()`, `writeRefreshCookie()`, `clearRefreshCookie()`, and `logout()` methods (logout is now handled entirely by Spring Security's `LogoutFilter` + `OidcClientInitiatedLogoutSuccessHandler`). Retain `register()` with all five Resilience4j annotations (`@CircuitBreaker`, `@Retry`, etc.) completely unchanged (Article VI). Depends on T012.

- [ ] T014 [US3] Modify `src/main/java/br/com/dealership/dealershibff/controller/AuthController.java` — remove `POST /api/v1/auth/logout` endpoint method (logout is now intercepted by Spring Security's `LogoutFilter` at `/api/v1/auth/logout` before the request reaches the controller). Retain `POST /api/v1/auth/register` with all OpenAPI annotations unchanged (Article XI). Depends on T012 and T013.

- [ ] T015 [US3] Delete `src/main/java/br/com/dealership/dealershibff/dto/response/TokenResponse.java` — no longer referenced after `AuthService.login()` is removed in T013. Confirm `mvn compile` passes after deletion. Depends on T013.

**⚑ Checkpoint — US3 Complete**

> Logout is fully operational. Validate independently:
> 1. `POST /api/v1/auth/logout` (authenticated) → `SESSION` cookie cleared → 302 to Keycloak `end_session`.
> 2. Subsequent request with old `SESSION` cookie → 401.
> 3. Keycloak admin: no active session for the logged-out user.
> 4. `POST /api/v1/auth/logout` (no valid session) → 401.
> 5. `mvn compile` passes with `LoginRequest.java`, `TokenResponse.java`, and `TokenRefreshFilter.java` all deleted.

---

## Phase 6: User Story 4 — Registration Unchanged (Priority: P4)

**Goal**: Confirm zero regression in `POST /api/v1/auth/register`. Update test files to reflect the removed endpoints, ensuring the registration test suite remains green.

**Independent Test**: `POST /api/v1/auth/register` with valid user data → user created in Keycloak via Admin API → 201 response with `ApiResponse<ClientApiClientResponse>` envelope, identical to pre-migration behaviour.

### Implementation for User Story 4

- [ ] T016 [P] [US4] Update `src/test/java/br/com/dealership/dealershibff/service/AuthServiceTest.java` — remove all test cases for `login()`, `refresh()`, `writeRefreshCookie()`, `clearRefreshCookie()`, and `logout()`; retain all test cases for `register()` unchanged; confirm all retained tests pass. Depends on T013.

- [ ] T017 [P] [US4] Update `src/test/java/br/com/dealership/dealershibff/controller/AuthControllerTest.java` — remove all test cases for `POST /login`, `POST /refresh`, and `POST /logout`; retain all test cases for `POST /register` unchanged; confirm all retained tests pass. Depends on T014.

**⚑ Checkpoint — US4 Complete**

> Registration regression confirmed. Validate:
> 1. `AuthServiceTest` passes — only `register()` tests remain; no compile errors from removed methods.
> 2. `AuthControllerTest` passes — only `POST /register` tests remain.
> 3. Full `mvn test` runs green with zero remaining references to deleted classes (`LoginRequest`, `TokenResponse`, `TokenRefreshFilter`).

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Integration test suite, observability verification, and final quality gates.

- [ ] T018 Create `src/test/java/br/com/dealership/dealershibff/integrated/auth/PkceAuthFlowIT.java` — full integration test class using WireMock (wiremock-spring-boot) for Keycloak stubs and Testcontainers for Redis; cover all acceptance scenarios from spec.md: (1) successful PKCE login redirect generates valid `code_challenge` + `state`; (2) callback with valid code → `SESSION` cookie set; (3) authenticated API call with session cookie → downstream `Authorization: Bearer` injected; (4) transparent access token refresh (stub 401 from downstream, then mock Keycloak `/token` refresh response); (5) session expiry / invalid cookie → 401; (6) logout → session invalidated → old cookie rejected → Keycloak `end_session` called with `id_token_hint`.

- [ ] T019 [P] Verify OpenAPI annotations on `src/main/java/br/com/dealership/dealershibff/controller/AuthController.java` — confirm `@Operation`, `@ApiResponse`, and `@Tag` annotations on `POST /api/v1/auth/register` are intact and accurate after all endpoint removals in T006 and T014 (Article XI).

- [ ] T020 [P] Run `quickstart.md` validation scenarios end-to-end against a local `compose.yaml` stack (Keycloak + Redis) — confirm all scenarios pass: PKCE login, session-based downstream call, token refresh, logout. Document any deviations from the quickstart guide.

---

## Dependencies & Execution Order

### Phase Dependencies

```
Phase 1 (Setup)
  └── T001 pom.xml deps
        └── Phase 2 (Foundational)
              └── T002 application.properties
                    ├── Phase 3 (US1) ──────────────── Checkpoint US1
                    │     ├── T003 [P] US1 unit tests (OAuth2LoginSuccessHandlerTest)
                    │     ├── T004 [P] Create OAuth2LoginSuccessHandler.java
                    │     ├── T005 SecurityConfig Part A (oauth2Login)
                    │     ├── T006 AuthController - remove /login, /refresh
                    │     └── T007 Delete LoginRequest.java
                    │
                    └── Phase 4 (US2) ──────────────── Checkpoint US2
                          ├── T008 [P] US2 unit tests (SessionTokenInjectionFilterTest)
                          ├── T009 [P] Create SessionTokenInjectionFilter.java
                          ├── T010 SecurityConfig Part B (swap filter)
                          └── T011 Delete TokenRefreshFilter.java
                                │
                                └── Phase 5 (US3) ──── Checkpoint US3
                                      ├── T012 SecurityConfig Part C (logout)
                                      ├── T013 AuthService - remove login/refresh/logout
                                      ├── T014 AuthController - remove /logout
                                      └── T015 Delete TokenResponse.java
                                              │
                                              └── Phase 6 (US4)
                                                    ├── T016 [P] AuthServiceTest cleanup
                                                    └── T017 [P] AuthControllerTest cleanup
                                                            │
                                                            └── Phase 7 (Polish)
                                                                  ├── T018 PkceAuthFlowIT
                                                                  ├── T019 [P] OpenAPI verify
                                                                  └── T020 [P] quickstart.md run
```

### User Story Dependencies

| Story | Depends On | Can Start When |
|---|---|---|
| US1 (P1) | T001, T002 | Phase 2 complete |
| US2 (P2) | T001, T002, US1 Checkpoint | US1 complete |
| US3 (P3) | US2 Checkpoint | US2 complete |
| US4 (P4) | US3 Checkpoint | US3 complete |

> **Note**: US1 → US2 → US3 are sequential because all three modify `SecurityConfig.java` in logically ordered parts. Parallelising them would cause merge conflicts and integration failures.

### Within Each User Story

- Unit tests (T003, T008) MUST be written before their implementation classes (T004, T009) and verified to FAIL
- New class creation before `SecurityConfig` wiring
- `SecurityConfig` wiring before dependent controller/service cleanup
- Cleanup (deletions) after all compile-time references are removed

### Parallel Opportunities

- **T003 + T004** (US1): Different files — `OAuth2LoginSuccessHandlerTest.java` and `OAuth2LoginSuccessHandler.java` can be drafted in parallel
- **T008 + T009** (US2): Different files — `SessionTokenInjectionFilterTest.java` and `SessionTokenInjectionFilter.java` can be drafted in parallel
- **T016 + T017** (US4): Different files — `AuthServiceTest.java` and `AuthControllerTest.java` can be updated in parallel
- **T019 + T020** (Polish): Independent of each other — can be done in parallel

---

## Parallel Example: User Story 1

```bash
# These two tasks have no dependency on each other — draft in parallel:
Task A: "Write unit tests for OAuth2LoginSuccessHandler in .../config/OAuth2LoginSuccessHandlerTest.java"
Task B: "Create OAuth2LoginSuccessHandler.java in .../config/ with full implementation"

# Then sequentially:
Task C: "Modify SecurityConfig.java Part A — oauth2Login + successHandler + sessionPolicy"
Task D: "Modify AuthController.java — remove /login and /refresh endpoints"
Task E: "Delete LoginRequest.java"
```

## Parallel Example: User Story 2

```bash
# These two tasks have no dependency on each other — draft in parallel:
Task A: "Write unit tests for SessionTokenInjectionFilter in .../web/SessionTokenInjectionFilterTest.java"
Task B: "Create SessionTokenInjectionFilter.java in .../web/ with full implementation"

# Then sequentially:
Task C: "Modify SecurityConfig.java Part B — wire SessionTokenInjectionFilter, remove TokenRefreshFilter"
Task D: "Delete TokenRefreshFilter.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Modify `pom.xml`
2. Complete Phase 2: Modify `application.properties`
3. Complete Phase 3: US1 — PKCE login (T003 → T004 → T005 → T006 → T007)
4. **⚑ STOP and VALIDATE**: Test PKCE login end-to-end in a browser
5. Demo / present the core security improvement (no password flows through BFF)

### Incremental Delivery

1. **Setup + Foundational** → POM + properties ready
2. **Add US1** → PKCE login works, SESSION cookie issued → ✅ Deploy / Demo (MVP!)
3. **Add US2** → Session-based downstream calls + transparent refresh → ✅ Deploy / Demo
4. **Add US3** → Full OIDC logout → ✅ Deploy / Demo
5. **Add US4** → Regression confirmed, test suite clean → ✅ Final release cut

### Single-Developer Sequence (Recommended)

```
T001 → T002 → T003 (tests FAIL) → T004 (tests PASS) → T005 → T006 → T007
→ [⚑ validate US1] →
T008 (tests FAIL) → T009 (tests PASS) → T010 → T011
→ [⚑ validate US2] →
T012 → T013 → T014 → T015
→ [⚑ validate US3] →
T016 ∥ T017
→ [⚑ validate US4] →
T018 → T019 ∥ T020
```

---

## Notes

- `[P]` tasks = different files, no incomplete shared-state dependencies
- `[USn]` label maps each task to the user story it delivers; traceability matrix:
  - US1: T003, T004, T005, T006, T007
  - US2: T008, T009, T010, T011
  - US3: T012, T013, T014, T015
  - US4: T016, T017
- **Constitution compliance** (Article VII): All fields in `OAuth2LoginSuccessHandler` and `SessionTokenInjectionFilter` must be `final`
- **Security** (Article X): Access tokens must never appear in log output; WARN log on refresh failure must include `requestId` from MDC only
- **Resilience** (Article VI): `register()` Resilience4j annotations must be preserved verbatim in T013; the accepted violation (OAuth2AuthorizedClientManager bypasses Feign/Resilience4j) is documented in plan.md Complexity Tracking
- Each checkpoint is a commit-and-test gate — stop, run `mvn test`, and validate the story's independent test criteria before proceeding
- If `mvn compile` fails after a deletion task (T007, T011, T015), the preceding cleanup task was incomplete — fix forward, do not restore the deleted file
