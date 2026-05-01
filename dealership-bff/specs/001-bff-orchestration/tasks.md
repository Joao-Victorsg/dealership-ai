# Tasks: BFF Orchestration Service

**Input**: Design documents from `/specs/001-bff-orchestration/`
**Prerequisites**: plan.md ✅ · spec.md ✅ · research.md ✅ · data-model.md ✅ · contracts/ ✅ · quickstart.md ✅

**Tests**: Included — spec.md success criteria SC-004 through SC-007 explicitly mandate ≥90% unit coverage, ≥90% mutation score, resilience integration tests, and security integration tests.

**Organization**: Tasks are grouped by user story (US1–US6 per spec.md priorities) to enable independent implementation and testing of each story increment.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no incomplete task dependencies in this phase)
- **[US#]**: Which user story this belongs to — omitted for Setup, Foundational, and Polish phases
- Exact file paths included in every task description

---

## Phase 1: Setup

**Purpose**: Project initialization — update dependencies, configure Maven plugins, set application properties, enable Feign.

**⚠️ CRITICAL**: All subsequent phases require these tasks to be complete. Do not start Phase 2 until Phase 1 is done.

- [X] T001 Update `pom.xml`: import Spring Cloud BOM 2025.1.0 in `dependencyManagement`; add `spring-cloud-starter-openfeign`, `spring-boot-starter-oauth2-resource-server`, `spring-boot-starter-validation`, `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.2`, `io.github.resilience4j:resilience4j-spring-boot4`, `org.instancio:instancio-junit:5.3.0`, `io.rest-assured:rest-assured:6.0.0`, `org.wiremock.integrations:wiremock-spring-boot:3.6.0`, `org.testcontainers:redis`; **remove** `spring-boot-starter-restdocs` and `spring-restdocs-mockmvc` (BFF uses springdoc, not REST Docs)

- [X] T002 Add Maven plugins to `pom.xml`: `org.pitest:pitest-maven:1.19.1` with `pitest-junit5-plugin:1.2.3`, `<features>+AUTO_THREADS</features>`, `<mutationThreshold>90</mutationThreshold>`, `<coverageThreshold>90</coverageThreshold>`, `<excludedTestClasses>integrated.**</excludedTestClasses>`, `<excludedClasses>` for main class + config.* + all envelope records, JVM arg `--add-opens=java.base/java.lang=ALL-UNNAMED`; `org.jacoco:jacoco-maven-plugin:0.8.13` with INSTRUCTION+BRANCH rules at 90%, excludes for main class + config.* + envelope records; `maven-failsafe-plugin` bound to `integration-test`/`verify` goals running classes matching `integrated.**`

- [X] T003 Configure `src/main/resources/application.properties`: server port 8084; Feign client URLs and timeouts for all four clients (car-api, client-api, sales-api, keycloak); `spring.security.oauth2.resourceserver.jwt.jwk-set-uri`; Redis host/port; `spring.cache.redis.time-to-live=300000`; `spring.threads.virtual.enabled=true`; Resilience4j aspect order properties (retry=1, circuitbreaker=2, ratelimiter=3, timelimiter=4, bulkhead=5); Actuator endpoints (health, info); springdoc path; all values backed by environment variable placeholders (e.g., `${CAR_API_BASE_URL:http://localhost:8081}`)

- [X] T004 Update `src/main/java/br/com/dealership/dealershibff/DealershiBffApplication.java`: add `@EnableFeignClients(basePackages = "br.com.dealership.dealershibff.feign")` to the main class annotation set

**Checkpoint**: pom.xml compiles with `./mvnw dependency:resolve`; `DealershiBffApplication` compiles; application.properties has all required stubs.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Cross-cutting infrastructure required by every user story: response envelope, exceptions, filters, security, caching, async, observability, and integration test base classes.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T005 [P] Create immutable envelope records in `src/main/java/br/com/dealership/dealershibff/dto/response/`: `ResponseMeta` (timestamp: Instant, requestId: String, page: Integer, pageSize: Integer, totalElements: Long, totalPages: Integer); `ApiResponse<T>` with static `of(T data, ResponseMeta meta)` and `paged(...)` factories; `ApiErrorResponse` with static `of(ErrorBody, ResponseMeta)` factory; `ErrorBody` (code: ErrorCode, message: String, details: List\<ErrorDetail\>); `ErrorDetail` (field: String, reason: String) — all as Java records

- [X] T006 [P] Create `src/main/java/br/com/dealership/dealershibff/domain/enums/ErrorCode.java`: enum with values `CAR_NOT_AVAILABLE`, `VALIDATION_ERROR`, `AUTHENTICATION_REQUIRED`, `FORBIDDEN`, `NOT_FOUND`, `RATE_LIMIT_EXCEEDED`, `DOWNSTREAM_UNAVAILABLE`, `DUPLICATE_IDENTITY`, `INTERNAL_ERROR`; each value annotated with its corresponding HTTP status

- [X] T007 [P] Create exception hierarchy in `src/main/java/br/com/dealership/dealershibff/domain/exception/`: `BffException extends RuntimeException` (ErrorCode field, HTTP status); `CarNotAvailableException`; `DownstreamServiceException`; `RegistrationException`; `DuplicateIdentityException`; `ForbiddenException`; `NotFoundException` — each mapping to its `ErrorCode`

- [X] T008 Create `src/main/java/br/com/dealership/dealershibff/config/GlobalExceptionHandler.java`: `@RestControllerAdvice` handling `BffException`, `MethodArgumentNotValidException` (→ `VALIDATION_ERROR` with field-level `ErrorDetail` list), `ConstraintViolationException`, `NoResourceFoundException`, `HttpMessageNotReadableException`, `Throwable` (fallback → `INTERNAL_ERROR`); all responses use `ApiErrorResponse`; requestId extracted from MDC; **never** expose stack traces or internal messages; `ResponseEntity<ApiErrorResponse>` return type on all handlers

- [X] T009 [P] Create `src/main/java/br/com/dealership/dealershibff/web/RequestIdFilter.java`: `OncePerRequestFilter` that generates a UUID if `X-Request-ID` header is absent (or uses existing value if present); stores in MDC under key `requestId`; makes available as a request attribute for use by controllers/exception handler; clears MDC on filter exit

- [X] T010 [P] Create `src/main/java/br/com/dealership/dealershibff/web/RequestLoggingFilter.java`: `OncePerRequestFilter` that logs after response: HTTP method, sanitized path (no query params logged if they contain sensitive data), response status code, latency (ms), authenticated subject from SecurityContext (or `"anonymous"`); uses SLF4J structured logging; `requestId` from MDC on every line; **never** logs request body

- [X] T011 [P] Create `src/main/java/br/com/dealership/dealershibff/web/InputSanitizationFilter.java`: `OncePerRequestFilter` that validates and normalizes: CPF (11 digits, check-digit algorithm), CEP (8 digits, strip hyphen), phone (10/11 Brazilian digits, strip formatting), search query `q` (strip non-alphanumeric except `.,'"-` and spaces, collapse whitespace); wraps `HttpServletRequest` with sanitized parameter map; rejects invalid CPF/CEP/phone with `400 VALIDATION_ERROR` envelope before service layer is reached; logs sanitized `q` only (never raw)

- [X] T012 Create `src/main/java/br/com/dealership/dealershibff/config/SecurityConfig.java`: `@Configuration @EnableMethodSecurity`; STATELESS session; OAuth2 Resource Server with JWT (`jwk-set-uri` from properties); `RolesClaimConverter` inner class extracting from `roles` or `realm_access.roles` claim with `ROLE_` prefix; public paths: `GET /api/v1/inventory/**`, `POST /api/v1/auth/login`, `POST /api/v1/auth/register`, `/swagger-ui/**`, `/v3/api-docs/**`, `/actuator/health/**`; all other paths require authentication; header stripping: remove `X-Internal-*` and `X-Forwarded-User` headers from inbound requests; `TokenRefreshFilter` registration deferred to T034 — leave a TODO comment

- [X] T013 [P] Create `src/main/java/br/com/dealership/dealershibff/config/AsyncConfig.java`: `@Configuration`; expose `@Bean Executor virtualThreadExecutor()` backed by `Thread.ofVirtual().factory()` (Java 25); annotate with `@Qualifier("virtualThreadExecutor")` for use in `PurchaseService`

- [X] T014 [P] Create `src/main/java/br/com/dealership/dealershibff/config/RedisConfig.java`: `@Configuration @EnableCaching`; `RedisCacheManager` bean with default TTL of 5 minutes; configure `StringRedisSerializer` for keys and `GenericJackson2JsonRedisSerializer` for values; register cache configs for `"car-by-id"` and `"car-listings"` with explicit 300-second TTL; handle Redis unavailability gracefully (log warning, do not throw on cache miss)

- [X] T015 [P] Create `src/main/java/br/com/dealership/dealershibff/config/OpenApiConfig.java`: `@Configuration`; configure `OpenAPI` bean with BFF API title, version, description; register Bearer token `SecurityScheme`; register all envelope types (`ApiResponse`, `ApiErrorResponse`, `ErrorBody`, `ResponseMeta`, `ErrorDetail`) as reusable OpenAPI components via `@Schema`; set base path `/api/v1`

- [X] T016 [P] Create `src/main/java/br/com/dealership/dealershibff/config/FeignConfig.java`: global `@Configuration` (not component-scanned by Feign — must be in the main package, not a sub-package of a Feign client); define global `Logger.Level` bean (`BASIC`); define global `Decoder` using Jackson `ObjectMapper` from Spring context; request interceptors registered per-client (not globally here)

- [X] T017 Create integration test infrastructure: `src/test/java/integrated/BaseIT.java` (`@SpringBootTest(webEnvironment = RANDOM_PORT)` + `@ActiveProfiles("test")` + RestAssured `baseURI` setup); `src/test/java/integrated/JwtTestUtils.java` (generates signed test JWTs with configurable sub, roles, email claims using Nimbus JOSE+JWT or `spring-security-test`'s JWT support); `src/test/java/integrated/EnvironmentInitializer.java` (registers Redis Testcontainer and the four WireMock servers via `@EnableWireMock({ @ConfigureWireMock(name="car-api-mock", property="feign.client.config.car-api.url"), ... })`); create `src/test/resources/application-test.properties` with test-specific overrides (test Redis host, test Keycloak JWKS URI pointing to keycloak-mock WireMock server)

- [X] T018 [P] Unit tests for `GlobalExceptionHandler` in `src/test/java/br/com/dealership/dealershibff/config/GlobalExceptionHandlerTest.java`: Mockito + MockMvc standaloneSetup; test each exception type maps to correct `ErrorCode` + HTTP status; verify `details` list populated for `VALIDATION_ERROR`; verify no stack trace in response body; verify `requestId` present in `meta`

- [X] T019 [P] Unit tests for web filters in `src/test/java/br/com/dealership/dealershibff/web/`: `RequestIdFilterTest.java` (MDC populated, UUID generated when header absent, existing header reused); `RequestLoggingFilterTest.java` (log entry produced after response, correct fields logged, no request body); `InputSanitizationFilterTest.java` (CPF normalised and validated, CEP normalised, phone normalised, invalid CPF → 400 envelope, malicious `q` stripped)

**Checkpoint**: `./mvnw test` passes — all filter and exception handler unit tests green; application starts with `spring-boot:run`.

---

## Phase 3: User Story 1 — Public Inventory Browsing and Search (Priority: P1) 🎯 MVP

**Goal**: Anonymous visitors can list and search car inventory via the BFF, with results cached in Redis, paginated and wrapped in the standard envelope.

**Independent Test**: Send unauthenticated `GET /api/v1/inventory` and `GET /api/v1/inventory/{id}` requests; verify paginated, enveloped results with no token required; verify 503 envelope when Car API WireMock is down.

- [X] T020 [P] [US1] Create Car API Feign DTOs in `src/main/java/br/com/dealership/dealershibff/feign/car/dto/`: `CarApiCarResponse` record (id, model, manufacturer, manufacturingYear, externalColor, internalColor, vin, status, category, type, isNew, kilometers, propulsionType, listedValue, imageKey, optionalItems, registrationDate); `CarApiPageResponse<T>` record (content: List\<T\>, totalElements: long, totalPages: int, number: int, size: int); `CarApiFilterParams` record (all filter, sort, pagination fields forwarded to Car API as query params)

- [X] T021 [P] [US1] Create `src/main/java/br/com/dealership/dealershibff/feign/car/CarApiErrorDecoder.java` (implements `ErrorDecoder`): map 404 → `NotFoundException`, 503 / `RetryableException` → `DownstreamServiceException`, other 4xx/5xx → `DownstreamServiceException` with message; create `src/main/java/br/com/dealership/dealershibff/feign/car/CarApiFeignConfig.java` (`@Configuration` registering `CarApiErrorDecoder` as `@Bean`)

- [X] T022 [US1] Create `src/main/java/br/com/dealership/dealershibff/feign/car/CarApiClient.java`: `@FeignClient(name = "car-api", url = "${feign.client.config.car-api.url}", configuration = CarApiFeignConfig.class)`; methods: `CarApiPageResponse<CarApiCarResponse> listCars(@SpringQueryMap CarApiFilterParams params)` → `GET /api/v1/cars`; `CarApiCarResponse getCarById(@PathVariable UUID id)` → `GET /api/v1/cars/{id}`

- [X] T023 [P] [US1] Create BFF response DTO `src/main/java/br/com/dealership/dealershibff/dto/response/VehicleResponse.java`: immutable record with all Car fields mapped from `CarApiCarResponse`; add static `VehicleResponse from(CarApiCarResponse source)` factory method

- [X] T024 [US1] Create `src/main/java/br/com/dealership/dealershibff/dto/request/InventoryFilterRequest.java`: record with all filter, sort, pagination fields (q, category, type, condition, manufacturer, yearMin, yearMax, priceMin, priceMax, color, kmMin, kmMax, sortBy, sortDirection, page, size); add `String toCacheKey()` method producing a deterministic sorted URL-encoded string from all non-null fields; constructor validates priceMin ≤ priceMax and yearMin ≤ yearMax

- [X] T025 [US1] Create `src/main/java/br/com/dealership/dealershibff/service/InventoryService.java`: inject `CarApiClient`; `@Cacheable(value = "car-listings", key = "#filter.toCacheKey()")` on `ApiResponse<List<VehicleResponse>> list(InventoryFilterRequest filter)`; `@Cacheable(value = "car-by-id", key = "#carId")` on `ApiResponse<VehicleResponse> getById(UUID carId)`; map `CarApiPageResponse` → `ApiResponse` with pagination in `ResponseMeta`; catch `DownstreamServiceException` propagation (let `GlobalExceptionHandler` handle it); pull `requestId` from MDC for `ResponseMeta`

- [X] T026 [US1] Create `src/main/java/br/com/dealership/dealershibff/controller/InventoryController.java`: `@RestController @RequestMapping("/api/v1/inventory")`; `GET /` mapped to `list(@ModelAttribute InventoryFilterRequest filter)` → `200 ApiResponse<List<VehicleResponse>>`; `GET /{id}` → `200 ApiResponse<VehicleResponse>`; annotate all endpoints with `@Operation`, `@ApiResponse` referencing envelope schemas; no security annotation (public per `SecurityConfig`)

- [X] T027 [P] [US1] Unit tests for `InventoryService` in `src/test/java/br/com/dealership/dealershibff/service/InventoryServiceTest.java`: `@ExtendWith(MockitoExtension.class)`; mock `CarApiClient`; use Instancio for test data generation; test: happy path list (verifies mapping), happy path getById, `DownstreamServiceException` propagates, toCacheKey() produces deterministic key for same inputs in different order

- [X] T028 [P] [US1] Unit tests for `InventoryController` in `src/test/java/br/com/dealership/dealershibff/controller/InventoryControllerTest.java`: `MockMvc` standaloneSetup with `InventoryController`; mock `InventoryService`; test: 200 paginated list response with correct `meta` pagination fields, 200 single vehicle response, 404 → correct error envelope, 503 → `DOWNSTREAM_UNAVAILABLE` envelope

- [X] T029 [US1] Integration tests in `src/test/java/integrated/inventory/`: `InventoryListIT.java` (WireMock stubs car-api list endpoint; verify BFF returns paginated envelope; verify cache hit on second identical request — confirm car-api only called once; verify 503 envelope when car-api returns 500); `InventorySearchIT.java` (free-text query forwarded and sanitized; filters and sort params forwarded correctly; invalid filter range → 400 validation envelope)

**Checkpoint**: US1 fully functional — anonymous inventory browsing works end-to-end via WireMock.

---

## Phase 4: User Story 2 — User Login and Token Lifecycle (Priority: P2)

**Goal**: Registered users can log in (access token in body, refresh token in HttpOnly cookie), transparently refresh expired tokens, and log out.

**Independent Test**: `POST /api/v1/auth/login` returns access token + HttpOnly cookie; subsequent request with expired token triggers transparent refresh; `POST /api/v1/auth/logout` clears cookie.

- [X] T030 [P] [US2] Create Keycloak Feign DTOs in `src/main/java/br/com/dealership/dealershibff/feign/keycloak/dto/`: `KeycloakTokenResponse` record (access_token, refresh_token, expires_in, refresh_expires_in, token_type); `KeycloakCreateUserRequest` record (username, email, enabled, credentials: List\<KeycloakCredential\>); `KeycloakCredential` record (type, value, temporary)

- [X] T031 [P] [US2] Create `src/main/java/br/com/dealership/dealershibff/feign/keycloak/KeycloakErrorDecoder.java`: map 401 → `BffException(AUTHENTICATION_REQUIRED)`; 409 → `DuplicateIdentityException`; 5xx → `DownstreamServiceException`; create `src/main/java/br/com/dealership/dealershibff/feign/keycloak/KeycloakFeignConfig.java` registering the decoder as `@Bean`

- [X] T032 [US2] Create `src/main/java/br/com/dealership/dealershibff/feign/keycloak/KeycloakClient.java`: `@FeignClient(name = "keycloak", url = "${feign.client.config.keycloak.url}", configuration = KeycloakFeignConfig.class)`; methods: `KeycloakTokenResponse login(@RequestBody MultiValueMap<String, String> formBody)` → `POST /realms/{realm}/protocol/openid-connect/token` (form-encoded); `void logout(...)` → `POST /realms/{realm}/protocol/openid-connect/logout`; `KeycloakCreateUserRequest createUser(...)` → `POST /admin/realms/{realm}/users`; `void deleteUser(@PathVariable String userId)` → `DELETE /admin/realms/{realm}/users/{userId}`; realm value injected from `${keycloak.realm}`

- [X] T033 [P] [US2] Create `src/main/java/br/com/dealership/dealershibff/dto/request/LoginRequest.java` (email @NotBlank @Email, password @NotBlank) and `src/main/java/br/com/dealership/dealershibff/dto/response/TokenResponse.java` (accessToken: String) as immutable records

- [X] T034 [US2] Create `src/main/java/br/com/dealership/dealershibff/web/TokenRefreshFilter.java`: `OncePerRequestFilter`; if `Authorization` header is absent or JWT is expired AND refresh token cookie `refresh_token` is present: call `AuthService.refresh(cookieValue)` → receive new `KeycloakTokenResponse`; write new refresh token back as `HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth` cookie; inject new access token into `SecurityContextHolder` via `BearerTokenAuthenticationToken`; if refresh fails (expired/revoked): clear the stale cookie, let request proceed without auth (Spring Security will return 401); also update `SecurityConfig.java` at T012's `// TODO` to register this filter before `BearerTokenAuthenticationFilter`

- [X] T035 [US2] Create `src/main/java/br/com/dealership/dealershibff/service/AuthService.java` with `login()`, `logout()`, `refresh()` methods: `login(LoginRequest)` → call Keycloak ROPC token endpoint via `KeycloakClient`; return `TokenResponse` (access token); write refresh token cookie to `HttpServletResponse`; `logout(refreshToken)` → call Keycloak logout endpoint; clear cookie; `refresh(refreshToken)` → call Keycloak token refresh endpoint; return new `KeycloakTokenResponse`; all Resilience4j annotations on Keycloak calls (`@CircuitBreaker`, `@RateLimiter`, `@TimeLimiter`, `@Retry` — login/refresh ARE retryable; logout IS retryable); use `@Qualifier("virtualThreadExecutor")` for any async calls

- [X] T036 [US2] Create `src/main/java/br/com/dealership/dealershibff/controller/AuthController.java`: `@RestController @RequestMapping("/api/v1/auth")`; `POST /login` → `200 ApiResponse<TokenResponse>`; `POST /logout` → `204 ApiResponse<Void>`; `POST /refresh` → `200 ApiResponse<TokenResponse>`; inject `HttpServletResponse` for cookie writing; annotate all with `@Operation` + envelope `@ApiResponse` refs; no auth annotation on login/logout/refresh (public per SecurityConfig)

- [X] T037 [P] [US2] Unit tests for `AuthService` login/logout/refresh in `src/test/java/br/com/dealership/dealershibff/service/AuthServiceTest.java`: mock `KeycloakClient`, mock `HttpServletResponse`; use Instancio for `KeycloakTokenResponse`; test: login happy path (access token returned, cookie written), login invalid credentials (401 envelope), logout clears cookie, refresh happy path, refresh with revoked token (401 envelope), Keycloak `DownstreamServiceException` propagation

- [X] T038 [P] [US2] Unit tests for `AuthController` login/logout/refresh in `src/test/java/br/com/dealership/dealershibff/controller/AuthControllerTest.java`: `MockMvc` standaloneSetup; mock `AuthService`; test: 200 login response body has `accessToken`; 204 logout; 401 error envelope for bad credentials; `Set-Cookie` header verified on login response

- [X] T039 [P] [US2] Unit tests for `TokenRefreshFilter` in `src/test/java/br/com/dealership/dealershibff/web/TokenRefreshFilterTest.java`: mock `AuthService`; test: no-op when valid `Authorization` header present; refresh triggered when header absent and cookie present; new cookie written on successful refresh; stale cookie cleared on failed refresh; expired access token + valid cookie → refresh → proceeds

- [X] T040 [US2] Integration tests in `src/test/java/integrated/auth/`: `LoginIT.java` (WireMock stubs Keycloak token endpoint; verify access token in body, `HttpOnly` `refresh_token` cookie in response headers; verify 401 error envelope for bad credentials); `LogoutIT.java` (verify logout clears cookie; verify subsequent refresh with cleared cookie returns 401)

**Checkpoint**: US2 functional — login, transparent refresh, logout all work via WireMock Keycloak stubs.

---

## Phase 5: User Story 3 — User Registration (Priority: P3)

**Goal**: New users can register via a two-phase Keycloak + Client API orchestration with best-effort compensation on failure.

**Independent Test**: Submit `POST /api/v1/auth/register`; verify user exists in both WireMock stubs; simulate Client API failure and confirm Keycloak delete is called and error envelope returned.

- [X] T041 [P] [US3] Create Client API Feign DTOs in `src/main/java/br/com/dealership/dealershibff/feign/client/dto/`: `ClientApiClientResponse` record (id, keycloakId, firstName, lastName, cpf, phone, createdAt, deletedAt, address: ClientApiAddressResponse); `ClientApiAddressResponse` record (street, number, complement, neighborhood, city, state, cep); `ClientApiCreateRequest` record (keycloakId, firstName, lastName, cpf, phone, cep); `ClientApiUpdateRequest` record (firstName, lastName, phone, cep — all optional)

- [X] T042 [P] [US3] Create `src/main/java/br/com/dealership/dealershibff/feign/client/ClientApiErrorDecoder.java`: map 404 → `NotFoundException`, 409 → `DuplicateIdentityException`, 5xx → `DownstreamServiceException`; create `src/main/java/br/com/dealership/dealershibff/feign/client/ClientApiFeignConfig.java` registering the decoder as `@Bean`

- [X] T043 [US3] Create `src/main/java/br/com/dealership/dealershibff/feign/client/ClientApiClient.java`: `@FeignClient(name = "client-api", url = "${feign.client.config.client-api.url}", configuration = ClientApiFeignConfig.class)`; methods: `ClientApiClientResponse getMe(@RequestHeader("Authorization") String bearerToken)` → `GET /clients/me`; `ClientApiClientResponse create(@RequestBody ClientApiCreateRequest body)` → `POST /clients`; `ClientApiClientResponse update(@PathVariable UUID id, @RequestBody ClientApiUpdateRequest body)` → `PATCH /clients/{id}`

- [X] T044 [US3] Create `src/main/java/br/com/dealership/dealershibff/dto/request/RegisterRequest.java`: immutable record (email @NotBlank @Email, password @NotBlank @Size(min=8), firstName @NotBlank, lastName @NotBlank, cpf @NotBlank, phone @NotBlank, cep @NotBlank); CPF/phone/CEP format validated by `InputSanitizationFilter` before this record is bound

- [X] T045 [US3] Add `register(RegisterRequest)` method to `src/main/java/br/com/dealership/dealershibff/service/AuthService.java`: Phase 1: call `KeycloakClient.createUser(...)` → capture `keycloakSubjectId`; Phase 2: call `ClientApiClient.create(...)` using `keycloakSubjectId`; on Client API failure: call `KeycloakClient.deleteUser(keycloakSubjectId)` as compensation; on compensation failure: log `ERROR "Registration compensation failed; orphaned Keycloak user {} requestId={}"` with subject and MDC requestId; in all failure cases re-throw appropriate `BffException` for `GlobalExceptionHandler`; Sales API POST rule analogy — Keycloak `createUser` call decorated with `@CircuitBreaker`, `@Retry`, `@RateLimiter`, `@TimeLimiter`, `@Bulkhead`; Client API call decorated similarly

- [X] T046 [US3] Add `POST /api/v1/auth/register` to `src/main/java/br/com/dealership/dealershibff/controller/AuthController.java`: `@PostMapping("/register")` receiving `@RequestBody @Valid RegisterRequest`; returns `201 ApiResponse<Void>` on success; annotate with `@Operation` + `@ApiResponse` refs for 201, 400, 422, 503

- [X] T047 [P] [US3] Unit tests for `AuthService.register()` in `src/test/java/br/com/dealership/dealershibff/service/AuthServiceTest.java` (extend existing class): test: happy path (Keycloak creates user, Client API creates profile, returns void); Keycloak success + Client API failure → compensation Keycloak delete called → error propagated; compensation Keycloak delete also fails → ERROR log entry verified via Mockito; duplicate email → `DuplicateIdentityException` from Keycloak decoder → correct error propagated; invalid CPF caught before any downstream call (validated by filter)

- [X] T048 [P] [US3] Unit tests for `AuthController.register()` in `src/test/java/br/com/dealership/dealershibff/controller/AuthControllerTest.java` (extend existing class): test: 201 on success; 400 on `@Valid` failure (email format, blank password, blank name); 422 on `DuplicateIdentityException`; 503 on `DownstreamServiceException`

- [X] T049 [US3] Integration test `src/test/java/integrated/auth/RegisterIT.java`: WireMock stubs Keycloak create-user and Client API create-client; test: full happy path 201; invalid CPF → 400 with field detail before any WireMock call; Client API failure → Keycloak delete called (verify via WireMock verification), 503 returned; duplicate Keycloak user (409 from Keycloak stub) → 422 envelope

**Checkpoint**: US3 functional — registration orchestration and compensation work end-to-end via WireMock.

---

## Phase 6: User Story 4 — Client Profile View and Update (Priority: P4)

**Goal**: Authenticated users can read and update their profile; CPF update is rejected with a 400 envelope without calling the Client API.

**Independent Test**: Authenticated `GET /api/v1/profile` returns profile; `PATCH /api/v1/profile` updates allowed fields; `PATCH` with `cpf` field returns 400 without calling Client API WireMock.

- [X] T050 [P] [US4] Create BFF response DTOs in `src/main/java/br/com/dealership/dealershibff/dto/response/`: `AddressView` record (street, number, complement, neighborhood, city, state, cep); `ProfileResponse` record (id, firstName, lastName, cpf, email, phone, createdAt, address: AddressView); static `ProfileResponse from(ClientApiClientResponse source, String email)` factory

- [X] T051 [P] [US4] Create `src/main/java/br/com/dealership/dealershibff/dto/request/UpdateProfileRequest.java`: record (firstName @Size(max=100), lastName @Size(max=100), phone, cep — all optional, no CPF field); if `cpf` key appears in JSON body, Jackson must throw (configure `@JsonProperty` + record constructor validation to catch unknown field `cpf` and throw `ValidationException` with `ErrorDetail(field="cpf", reason="CPF cannot be updated through this endpoint")`)

- [X] T052 [US4] Create `src/main/java/br/com/dealership/dealershibff/service/ProfileService.java`: inject `ClientApiClient`; `getProfile(String bearerToken, String emailFromJwt)` → `ClientApiClient.getMe(bearerToken)` → map to `ProfileResponse.from(...)` injecting email from JWT; `updateProfile(UUID clientId, UpdateProfileRequest request, String bearerToken, String emailFromJwt)` → `ClientApiClient.update(clientId, mapped request)` → return updated `ProfileResponse`; subject extracted from `Authentication` in service method; all Client API calls decorated with Resilience4j annotations (`@CircuitBreaker`, `@Retry`, `@TimeLimiter`, `@RateLimiter`, `@Bulkhead`)

- [X] T053 [US4] Create `src/main/java/br/com/dealership/dealershibff/controller/ProfileController.java`: `@RestController @RequestMapping("/api/v1/profile") @PreAuthorize("hasRole('CLIENT')")`; `GET /` → `200 ApiResponse<ProfileResponse>` (extract token from `Authentication`, email from JWT claims); `PATCH /` → `200 ApiResponse<ProfileResponse>` (`@RequestBody @Valid UpdateProfileRequest`); extract subject/email from `JwtAuthenticationToken` for service calls; annotate with `@Operation` + `@ApiResponse` refs

- [X] T054 [P] [US4] Unit tests for `ProfileService` in `src/test/java/br/com/dealership/dealershibff/service/ProfileServiceTest.java`: mock `ClientApiClient`; Instancio for `ClientApiClientResponse`; test: `getProfile` happy path (email injected from JWT), `updateProfile` happy path, `NotFoundException` propagation, `DownstreamServiceException` propagation

- [X] T055 [P] [US4] Unit tests for `ProfileController` in `src/test/java/br/com/dealership/dealershibff/controller/ProfileControllerTest.java`: `MockMvc` with `@WithMockUser`; test: 200 GET profile, 200 PATCH allowed fields, 400 on `cpf` field in PATCH body (verify Client API never called), 401 unauthenticated access, 403 wrong role

- [X] T056 [US4] Integration test `src/test/java/integrated/profile/ProfileIT.java`: WireMock stubs Client API `GET /clients/me` and `PATCH /clients/{id}`; test: authenticated GET returns profile with email from JWT; PATCH updates fields and returns updated profile; PATCH with `cpf` key → 400 envelope, no WireMock call to client-api; unauthenticated access → 401 envelope

**Checkpoint**: US4 functional — profile read and update work with valid JWT; CPF protection enforced.

---

## Phase 7: User Story 5 — Car Purchase (Priority: P5)

**Goal**: Authenticated users can purchase an available car; BFF fetches car + client data in parallel, submits to Sales API, evicts cache; `CAR_NOT_AVAILABLE` returned on race condition.

**Independent Test**: `POST /api/v1/purchases` for an available car returns 201 with purchase response; repeat request with Sales API returning 409 returns `CAR_NOT_AVAILABLE` envelope; verify `car-by-id` cache evicted after success.

- [X] T057 [P] [US5] Create Sales API Feign DTOs in `src/main/java/br/com/dealership/dealershibff/feign/sales/dto/`: `SalesApiCarSnapshotRequest` record (model, manufacturer, externalColor, internalColor, manufacturingYear, optionalItems, type, category, vin, listedValue, status); `SalesApiClientSnapshotRequest` record (firstName, lastName, cpf, email, address: SalesApiAddressSnapshotRequest); `SalesApiAddressSnapshotRequest` record (street, number, complement, neighborhood, city, state, cep); `SalesApiRegisterRequest` record (carId: UUID, clientId: UUID, clientSnapshot, carSnapshot); `SalesApiSaleResponse` record (id, carId, clientId, carSnapshot, clientSnapshot, status, registeredAt); `SalesApiPageResponse<T>` record (content, totalElements, totalPages, number, size)

- [X] T058 [P] [US5] Create `src/main/java/br/com/dealership/dealershibff/feign/sales/SalesApiErrorDecoder.java`: map 409 → `CarNotAvailableException`; 429 → `BffException(RATE_LIMIT_EXCEEDED)`; 5xx → `DownstreamServiceException`; create `src/main/java/br/com/dealership/dealershibff/feign/sales/SalesApiFeignConfig.java` registering decoder as `@Bean`

- [X] T059 [US5] Create `src/main/java/br/com/dealership/dealershibff/feign/sales/SalesApiClient.java`: `@FeignClient(name = "sales-api", url = "${feign.client.config.sales-api.url}", configuration = SalesApiFeignConfig.class)`; methods: `SalesApiSaleResponse registerSale(@RequestBody SalesApiRegisterRequest body)` → `POST /api/v1/sales`; `SalesApiPageResponse<SalesApiSaleResponse> listSales(@RequestHeader("Authorization") String token, @SpringQueryMap Map<String, Object> params)` → `GET /api/v1/sales/me`

- [X] T060 [P] [US5] Create BFF DTOs in `src/main/java/br/com/dealership/dealershibff/dto/`: `PurchaseRequest` record in `request/` (carId: @NotNull UUID); `VehicleSnapshot` record in `response/` (id, model, manufacturer, manufacturingYear, externalColor, vin, category, listedValue); `ClientSnapshot` record in `response/` (firstName, lastName, cpf); `PurchaseResponse` record in `response/` (id, registeredAt, status, vehicle: VehicleSnapshot, client: ClientSnapshot); static `PurchaseResponse from(SalesApiSaleResponse)` factory

- [X] T061 [US5] Create `src/main/java/br/com/dealership/dealershibff/service/PurchaseService.java`: inject `CarApiClient`, `ClientApiClient`, `SalesApiClient`; `@Qualifier("virtualThreadExecutor") Executor executor`; `purchase(UUID carId, Authentication auth)`: (1) verify car available via `CarApiClient.getCarById(carId)` → throw `CarNotAvailableException` if status ≠ AVAILABLE; (2) `CompletableFuture<CarApiCarResponse> carFuture = supplyAsync(() → carApiClient.getCarById(carId), executor)`; `CompletableFuture<ClientApiClientResponse> clientFuture = supplyAsync(() → clientApiClient.getMe(token), executor)`; `CompletableFuture.allOf(carFuture, clientFuture).exceptionally(ex → { carFuture.cancel(true); clientFuture.cancel(true); throw wrap(ex); }).join()`; (3) assemble `SalesApiRegisterRequest` with full snapshots; (4) `@CircuitBreaker @RateLimiter @TimeLimiter @Bulkhead` (NO `@Retry`) on `salesApiClient.registerSale(...)` call; (5) `@CacheEvict(value = "car-by-id", key = "#carId")`; return `PurchaseResponse.from(...)`

- [X] T062 [US5] Create `src/main/java/br/com/dealership/dealershibff/controller/PurchaseController.java`: `@RestController @RequestMapping("/api/v1/purchases") @PreAuthorize("hasRole('CLIENT')")`; `POST /` → `201 ApiResponse<PurchaseResponse>` (`@RequestBody @Valid PurchaseRequest`); inject `Authentication` for subject/token extraction; annotate with `@Operation` + `@ApiResponse` refs for 201, 400, 401, 403, 404, 409, 429, 503

- [X] T063 [P] [US5] Unit tests for `PurchaseService.purchase()` in `src/test/java/br/com/dealership/dealershibff/service/PurchaseServiceTest.java`: mock `CarApiClient`, `ClientApiClient`, `SalesApiClient`; use real `virtualThreadExecutor` in test; test: happy path (201, cache evict called), car status ≠ AVAILABLE → `CarNotAvailableException` before parallel fetch, parallel fetch failure (client) → other future cancelled → `DownstreamServiceException`, Sales API 409 → `CarNotAvailableException` (no retry — verify `SalesApiClient.registerSale` called exactly once), Sales API 5xx → `DownstreamServiceException` (no retry — verify exactly once call), parallel execution verified (mock both futures to take 100ms; assert total < 150ms)

- [X] T064 [P] [US5] Unit tests for `PurchaseController` in `src/test/java/br/com/dealership/dealershibff/controller/PurchaseControllerTest.java`: `MockMvc` with `@WithMockUser`; mock `PurchaseService`; test: 201 purchase response, 400 missing carId, 409 `CAR_NOT_AVAILABLE` envelope, 503 `DOWNSTREAM_UNAVAILABLE` envelope, 401 unauthenticated, 403 wrong role

- [X] T065 [US5] Integration test `src/test/java/integrated/purchase/PurchaseIT.java`: WireMock stubs car-api (GET available car, GET full car), client-api (GET me), sales-api (POST /sales success and 409); test: full happy path 201 + verify both parallel WireMock calls received; 409 race condition → `CAR_NOT_AVAILABLE` envelope; Sales API called exactly once (no retry); cache eviction verified (second GET to car-api after purchase is a cache miss — car-api WireMock receives a second request)

**Checkpoint**: US5 functional — purchase flow with parallel assembly, race condition handling, and cache eviction all verified.

---

## Phase 8: User Story 6 — Purchase History (Priority: P6)

**Goal**: Authenticated users can retrieve their paginated purchase history; data is never cached.

**Independent Test**: `GET /api/v1/purchases` returns paginated history; empty list returns `[]` not `null`; verify no Redis cache used (Sales API WireMock always called).

- [X] T066 [US6] Add `history(Authentication auth, int page, int size, Instant from, Instant to)` method to `src/main/java/br/com/dealership/dealershibff/service/PurchaseService.java`: call `SalesApiClient.listSales(token, params)` with JWT from auth; map `SalesApiPageResponse<SalesApiSaleResponse>` → `ApiResponse<List<PurchaseResponse>>` with pagination `ResponseMeta`; decorate with `@CircuitBreaker`, `@Retry`, `@RateLimiter`, `@TimeLimiter`, `@Bulkhead`; **no `@Cacheable`** — history is never cached

- [X] T067 [US6] Add `GET /api/v1/purchases` to `src/main/java/br/com/dealership/dealershibff/controller/PurchaseController.java`: `@GetMapping` receiving optional `from`, `to`, `page`, `size` query params; returns `200 ApiResponse<List<PurchaseResponse>>` with pagination meta; requires `ROLE_CLIENT`; annotate with `@Operation` + `@ApiResponse` refs for 200, 401, 403, 503

- [X] T068 [P] [US6] Unit tests for `PurchaseService.history()` in `src/test/java/br/com/dealership/dealershibff/service/PurchaseServiceTest.java` (extend existing class): test: happy path paginated list, empty list returns `[]` not `null`, `DownstreamServiceException` propagation, pagination meta fields correctly populated in `ResponseMeta`

- [X] T069 [P] [US6] Unit tests for `PurchaseController.history()` in `src/test/java/br/com/dealership/dealershibff/controller/PurchaseControllerTest.java` (extend existing class): test: 200 paginated response, 200 with empty list (`data: []`), 401 unauthenticated, 403 wrong role, 503 downstream error envelope

- [X] T070 [US6] Integration test `src/test/java/integrated/purchase/PurchaseHistoryIT.java`: WireMock stubs Sales API `GET /api/v1/sales/me` with paginated response; test: authenticated request returns paginated history; no purchases → `data: []`; unauthenticated → 401 envelope; Sales API always called (verify WireMock receives request on every history call — confirm no caching)

**Checkpoint**: US6 functional — history retrieval works, never served from cache.

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Complete security and resilience integration test coverage (SC-006, SC-007), finalize Resilience4j instance configuration, and verify full build thresholds.

- [X] T071 [P] Security integration tests in `src/test/java/integrated/security/`: `UnauthenticatedAccessIT.java` — verify all protected endpoints (`/api/v1/profile`, `/api/v1/purchases`, `POST /api/v1/purchases`) return 401 `AUTHENTICATION_REQUIRED` envelope without a token; `ForbiddenRoleIT.java` — verify endpoints requiring `ROLE_CLIENT` return 403 `FORBIDDEN` envelope when a token with a different role is presented; `InputSanitizationIT.java` — verify invalid CPF (`POST /register`), invalid CEP (`PATCH /profile`), malicious search query (`GET /inventory?q=<script>`), and phone format errors each produce 400 `VALIDATION_ERROR` envelopes with correct `details`; verify `HttpOnly` flag on refresh token cookie via response header inspection (SC-009)

- [X] T072 [P] Resilience integration tests in `src/test/java/integrated/resilience/`: `CircuitBreakerIT.java` — WireMock simulates car-api returning 500 repeatedly until circuit opens; verify state transition and `DOWNSTREAM_UNAVAILABLE` envelope; `RetryExhaustionIT.java` — WireMock returns 5xx 3 times for a retryable endpoint; verify retry exhaustion and final error envelope; `RateLimiterIT.java` — send rapid requests exceeding rate limit; verify `RATE_LIMIT_EXCEEDED` envelope for excess requests; `BulkheadIT.java` — saturate sales-api bulkhead semaphore; verify `DOWNSTREAM_UNAVAILABLE` envelope for rejected requests; `SC-010` concurrency test: verify parallel purchase assembly runs in parallel (elapsed < sum of individual durations)

- [X] T073 Complete Resilience4j per-service instance configuration in `src/main/resources/application.properties`: add full config for `car-api`, `client-api`, `sales-api`, `keycloak` instances — circuit breaker (sliding window size, failure rate threshold, wait duration in open state, permitted calls in half-open); retry (max attempts, wait duration, retry exceptions); rate limiter (limit for period, limit refresh period, timeout duration); time limiter (timeout duration); bulkhead (max concurrent calls, max wait duration — semaphore type); sales-api bulkhead configured with lower concurrency to protect against surges; all values externalized (no hardcoded defaults in Java code)

- [X] T074 Create `src/test/resources/application-test.properties`: override `spring.security.oauth2.resourceserver.jwt.jwk-set-uri` to point to `${keycloak-mock.url}/realms/dealership/protocol/openid-connect/certs`; disable Resilience4j time limits and retry delays for faster integration test execution; set Redis to use Testcontainer-managed host/port; set `spring.jpa.show-sql=false`; `logging.level.root=WARN` (reduce noise in test output)

- [X] T075 Verify full build: run `./mvnw verify` and confirm: all unit tests green; JaCoCo report shows ≥90% INSTRUCTION and ≥90% BRANCH for application code; PITest report shows ≥90% mutation score; Failsafe integration tests green; no raw Spring error format in any test response; fix any threshold violations before marking complete

**Final Checkpoint**: `./mvnw verify` green — 90% JaCoCo + 90% PITest + all integration tests passing.

---

## Dependency Graph

```
Phase 1 (Setup: T001–T004)
    └─→ Phase 2 (Foundational: T005–T019)
            ├─→ Phase 3 US1 (Inventory: T020–T029)      # independent after Phase 2
            ├─→ Phase 4 US2 (Login/Token: T030–T040)    # independent after Phase 2
            │       └─→ Phase 5 US3 (Register: T041–T049)  # needs KeycloakClient from US2
            │               └─→ Phase 6 US4 (Profile: T050–T056)  # needs ClientApiClient from US3
            │                       └─→ Phase 7 US5 (Purchase: T057–T065)  # needs all above
            │                               └─→ Phase 8 US6 (History: T066–T070)  # needs SalesApiClient from US5
            └─→ Phase 9 (Polish: T071–T075)  # runs after US6
```

**Cross-story dependency note**: Phase 5 (US3 Registration) needs `ClientApiClient` but Phase 6 (US4 Profile) also needs it. US3 creates it; US4 reuses it. Implement US3 first.

---

## Parallel Execution Examples Per Story

### US1 — After Phase 2 complete:
```
T020 (Car Feign DTOs)    ──┐
T021 (ErrorDecoder+Config) ─┼→ T022 (CarApiClient) → T024 (InventoryFilterRequest) → T025 (InventoryService) → T026 (InventoryController)
T023 (VehicleResponse)   ──┘
                                                      T027 (InventoryService tests) ─┐
                                                      T028 (InventoryController tests) ┤→ T029 (Integration tests)
```

### US2 — After Phase 2 complete (in parallel with US1):
```
T030 (Keycloak DTOs)     ──┐
T031 (ErrorDecoder+Config) ─┼→ T032 (KeycloakClient) → T034 (TokenRefreshFilter) → T035 (AuthService) → T036 (AuthController)
T033 (LoginRequest DTOs) ──┘
                             T037 (AuthService tests)     ─┐
                             T038 (AuthController tests)   ┤→ T040 (Integration tests)
                             T039 (TokenRefreshFilter test)┘
```

### US5 — After US3 + US4 complete:
```
T057 (Sales Feign DTOs)  ──┐
T058 (ErrorDecoder+Config) ─┼→ T059 (SalesApiClient) ──┐
T060 (Purchase DTOs)     ──┘                           ├→ T061 (PurchaseService) → T062 (PurchaseController)
                                                       └─ (reuses CarApiClient + ClientApiClient from prior stories)
                                                          T063 (PurchaseService tests) ─┐
                                                          T064 (PurchaseController tests)┤→ T065 (Integration tests)
```

---

## Implementation Strategy

### MVP Scope (deliver value fast)
Implement **Phase 3 (US1)** first. Anonymous inventory browsing requires no auth infrastructure beyond what Phase 2 provides. It is independently demonstrable, delivers the top-of-funnel feature, and validates the Feign + Redis + envelope pipeline end-to-end.

### Incremental Delivery Order
1. **US1** (Inventory) — Public, no auth, validates cache + envelope pipeline
2. **US2** (Login/Token) — Auth foundation unlocks all personalized features
3. **US3** (Registration) — Creates users, depends on US2's Keycloak client
4. **US4** (Profile) — Read/update, depends on US3's Client API client
5. **US5** (Purchase) — Highest business value, depends on US2+US4+US1's Car API client
6. **US6** (History) — Read-only convenience, reuses US5's Sales API client

### Testing Approach
- Write unit tests immediately after each implementation task in the same story phase
- Run `./mvnw test` after each story phase before moving to the next
- Do not defer integration tests to Phase 9 — each story phase includes its own integration test task

---

## Summary

| Phase | Tasks | Story | Parallel Opportunities |
|-------|-------|-------|------------------------|
| 1 — Setup | T001–T004 | — | T001+T002 (separate pom.xml edits — do sequentially) |
| 2 — Foundational | T005–T019 | — | T005, T006, T007, T009, T010, T011, T013, T014, T015, T016, T018, T019 |
| 3 — US1 Inventory | T020–T029 | US1 | T020, T021, T023 (then T022 after DTOs) |
| 4 — US2 Login/Token | T030–T040 | US2 | T030, T031, T033 (then T032); T037, T038, T039 |
| 5 — US3 Registration | T041–T049 | US3 | T041, T042 (then T043); T047, T048 |
| 6 — US4 Profile | T050–T056 | US4 | T050, T051; T054, T055 |
| 7 — US5 Purchase | T057–T065 | US5 | T057, T058, T060 (then T059); T063, T064 |
| 8 — US6 History | T066–T070 | US6 | T068, T069 |
| 9 — Polish | T071–T075 | — | T071, T072 |
| **Total** | **75 tasks** | | |

**Task count per user story**: US1=10, US2=11, US3=9, US4=7, US5=9, US6=5
**Suggested MVP**: Phase 1 + Phase 2 + Phase 3 (US1) = 29 tasks → anonymous inventory browsing fully functional
