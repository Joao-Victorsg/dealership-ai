# Tasks: Sales API

**Input**: Design documents from `specs/001-sales-api/`
**Prerequisites**: plan.md ✅ · spec.md ✅ · research.md ✅ · data-model.md ✅ · contracts/ ✅ · quickstart.md ✅

**Branch**: `001-sales-api` | **Date**: 2026-04-20

---

## Format: `[ID] [P?] [Story?] Description — file path`

- **[P]**: Parallelizable (different files, no dependency on any incomplete task in the same phase)
- **[US1/US2/US3]**: The user story this task delivers
- Tasks within each story phase follow TDD order: tests first, then implementation
- All paths are relative to the repository root

---

## Phase 1: Setup

**Purpose**: Update the Maven project with all required dependencies/plugins and configure application properties.

- [X] T001 Update `pom.xml` with all required dependencies: AWS SDK v2 BOM (`software.amazon.awssdk:bom:2.25.60`), `resilience4j-spring-boot3:2.3.0`, `spring-boot-starter-aop`, `spring-boot-starter-oauth2-resource-server`, `spring-boot-starter-data-redis`, `springdoc-openapi-starter-webmvc-ui:3.0.2`, `software.amazon.awssdk:sns`, `software.amazon.awssdk:sqs` (test scope), `instancio-junit:5.3.0` (test), `rest-assured:6.0.0` (test), `testcontainers-bom` (import), `org.testcontainers:localstack` (test), `org.wiremock.integrations:wiremock-testcontainers-module:1.0-alpha-14` (test), `com.nimbusds:nimbus-jose-jwt:9.48` (test); and plugins: `jacoco-maven-plugin:0.8.13` (line+branch ≥90%), `pitest-maven:1.17.0` (mutation ≥90%), `maven-failsafe-plugin` with `**/*IT.java` includes and Surefire exclusion of `**/integrated/**` — in `pom.xml`
- [X] T002 Configure all required properties in `src/main/resources/application.properties`: `spring.jpa.open-in-view=false`, `spring.jpa.hibernate.ddl-auto=validate`, `spring.jpa.properties.hibernate.jdbc.time_zone=UTC`, `spring.threads.virtual.enabled=true`, `spring.security.oauth2.resourceserver.jwt.jwks-uri`, `spring.security.oauth2.resourceserver.jwt.audiences=dealership`, `spring.datasource.*`, `spring.data.redis.*`, `app.sns.endpoint-override`, `app.sns.topic-arn`, `resilience4j.retry.instances.sns.*` (3 attempts, 500ms wait, `SnsPublishException`), `resilience4j.circuitbreaker.instances.sns.*` (50% failure threshold, 5 calls), `resilience4j.retry.retry-aspect-order=2`, `resilience4j.circuitbreaker.circuit-breaker-aspect-order=3`, `springdoc.api-docs.path=/api-docs`, `spring.jackson.default-property-inclusion=NON_NULL`, `management.endpoint.health.probes.enabled=true`, `management.endpoints.web.exposure.include=health,info,metrics` — in `src/main/resources/application.properties`

**Checkpoint**: All dependencies resolvable (`mvn dependency:resolve`); application compiles.

---

## Phase 2: Foundational

**Purpose**: All blocking infrastructure that MUST be complete before any user story. Includes DB schema, domain model, shared infrastructure beans, security, and IT base classes.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T003 [P] Create Flyway migration with `sales` table (UUID PK, `car_id` UNIQUE, `client_id`, `sale_value NUMERIC(19,4)`, `registered_at TIMESTAMPTZ DEFAULT NOW()`, `client_snapshot JSONB`, `car_snapshot JSONB`), `CONSTRAINT chk_sales_value CHECK (sale_value > 0)`, `CONSTRAINT chk_car_snapshot_status CHECK ((car_snapshot->>'status') IN ('AVAILABLE', 'SOLD', 'UNAVAILABLE'))`, and indexes on `client_id`, `registered_at`, composite `(client_id, registered_at)` — in `src/main/resources/db/migration/V1__create_sales_table.sql`
- [X] T004 [P] Create `CarStatus` enum with values `AVAILABLE`, `SOLD`, `UNAVAILABLE` — in `src/main/java/br/com/dealership/salesapi/domain/entity/CarStatus.java`
- [X] T005 [P] Create `AddressSnapshot` record with fields: `street`, `number`, `complement` (nullable), `neighborhood`, `city`, `state`, `postcode` (all `String`) — in `src/main/java/br/com/dealership/salesapi/domain/entity/AddressSnapshot.java`
- [X] T006 [P] Create `ClientSnapshot` record with fields: `firstName`, `lastName`, `cpf`, `email` (all `String`), `address` (`AddressSnapshot`) — in `src/main/java/br/com/dealership/salesapi/domain/entity/ClientSnapshot.java`
- [X] T007 [P] Create `CarSnapshot` record with fields: `model`, `manufacturer`, `externalColor`, `internalColor` (String), `manufacturingYear` (Integer), `optionalItems` (List<String>), `type`, `category`, `vin`, (String), `listedValue` (BigDecimal), `status` (CarStatus); compact constructor must call `List.copyOf()` on `optionalItems` (defaulting to `List.of()` if null) — in `src/main/java/br/com/dealership/salesapi/domain/entity/CarSnapshot.java`
- [X] T008 Create `Sale` JPA entity: `@Entity`, `@Table(name="sales", uniqueConstraints=@UniqueConstraint(name="uk_sales_car_id", columnNames="car_id"))`, `@NoArgsConstructor(PROTECTED)`, `@AllArgsConstructor(PRIVATE)`, `@Builder`, no `@Setter`; fields `id` (UUID, `@Id`, not updatable), `carId`, `clientId` (UUID), `saleValue` (BigDecimal, precision=19 scale=4), `registeredAt` (Instant), `clientSnapshot` (`@JdbcTypeCode(SqlTypes.JSON)`, columnDefinition="jsonb"), `carSnapshot` (`@JdbcTypeCode(SqlTypes.JSON)`, columnDefinition="jsonb"); all fields `updatable=false`; public read-only accessors only — in `src/main/java/br/com/dealership/salesapi/domain/entity/Sale.java`
- [X] T009 Create `SaleRepository` extending `JpaRepository<Sale, UUID>` and `JpaSpecificationExecutor<Sale>`; declare `Page<Sale> findByClientId(UUID clientId, Pageable pageable)` and `Page<Sale> findByClientIdAndRegisteredAtBetween(UUID clientId, Instant from, Instant to, Pageable pageable)` — in `src/main/java/br/com/dealership/salesapi/repository/SaleRepository.java`
- [X] T010 [P] Create sealed `SalesApiException extends RuntimeException permits CarNotAvailableException, SaleOwnershipException, CarAlreadySoldException, SaleNotFoundException, SnsPublishException`; create all five `final` leaf classes with single-string-message constructors — in `src/main/java/br/com/dealership/salesapi/domain/exception/` (one file per class)
- [X] T011 [P] Create `Response<T>` record (`data` field) with `static <T> Response<T> of(T data) { return new Response<>(data); }` factory; `ErrorResponse` record (`@Builder`, `message`, `List<FieldError> errors`); `FieldError` record (`field`, `message`) with `static FieldError of(String field, String message)` factory — in `src/main/java/br/com/dealership/salesapi/dto/response/`
- [X] T012 Create `GlobalExceptionHandler` (`@RestControllerAdvice`): exhaustive `switch` pattern-matching on the sealed `SalesApiException` hierarchy mapping to HTTP status codes (CarNotAvailable→422, SaleOwnership→403, CarAlreadySold→422, SaleNotFound→404, SnsPublish→503); handler for `MethodArgumentNotValidException` → 400 with `List<FieldError>` from binding result; handler for `IllegalArgumentException` → 400 with `ErrorResponse(message = ex.getMessage())` (covers compact constructor validation failures — constitution Art. XIII); handler for generic `Exception` → 500 — in `src/main/java/br/com/dealership/salesapi/config/GlobalExceptionHandler.java`
- [X] T013 [P] Create `SecurityConfig` (`@EnableWebSecurity`, `@EnableMethodSecurity`): stateless session, CSRF disabled, all requests authenticated, configure `JwtAuthenticationConverter` with inner `KeycloakRolesConverter` that extracts `realm_access.roles` from JWT claims and prefixes each role with `ROLE_`, permit Actuator `/actuator/health/**` and `/actuator/info`; audience validation via `spring.security.oauth2.resourceserver.jwt.audiences` — in `src/main/java/br/com/dealership/salesapi/config/SecurityConfig.java`
- [X] T014 [P] Create `SnsProperties` `@ConfigurationProperties(prefix = "app.sns")` record with fields `endpointOverride` (String) and `topicArn` (String); annotate main application class with `@ConfigurationPropertiesScan` — in `src/main/java/br/com/dealership/salesapi/messaging/SnsProperties.java`
- [X] T015 Create `SnsConfig` `@Configuration` bean that creates `SnsClient`: inject `SnsProperties`, use `DefaultCredentialsProvider`, set `endpointOverride` when `endpointOverride` is non-blank, `apiCallTimeout(Duration.ofSeconds(2))`, `apiCallAttemptTimeout(Duration.ofSeconds(1))`, region from environment — in `src/main/java/br/com/dealership/salesapi/config/SnsConfig.java`
- [X] T016 [P] Create `RedisConfig` (`@Configuration`, `@EnableCaching`): define `RedisCacheManager` bean using `RedisCacheManagerBuilder`; configure a `"sales"` cache with `GenericJacksonJsonRedisSerializer`, 24h TTL, `disableCachingNullValues()` — in `src/main/java/br/com/dealership/salesapi/config/RedisConfig.java`
- [X] T017 [P] Create `OpenApiConfig` (`@Configuration`): define `OpenAPI` bean with title "Sales API", version "v1", and `SecurityScheme` of type HTTP bearer with bearerFormat JWT named `"bearerAuth"` — in `src/main/java/br/com/dealership/salesapi/config/OpenApiConfig.java`
- [X] T018 [P] Create `RequestLoggingFilter` extending `OncePerRequestFilter`: capture request method, path, query string, and response status + latency; set MDC keys `http.method`, `http.path`, `http.status`, `http.latency_ms`; clear MDC in `finally` — in `src/main/java/br/com/dealership/salesapi/web/RequestLoggingFilter.java`
- [X] T019 [P] Create `BaseIT` (`@SpringBootTest(webEnvironment=RANDOM_PORT)`, `@ActiveProfiles("test")`): declare three static Testcontainers containers (`PostgreSQLContainer postgres:17-alpine`, `WireMockContainer wiremock/wiremock:3.13.0`, `LocalStackContainer localstack/localstack:4.4` with `Service.SNS` and `Service.SQS`); generate static `RSAKey TEST_RSA_KEY` (RSA-2048, Nimbus) at class load; implement `buildToken(UUID subject, String... roles)` helper that signs a JWT with `TEST_RSA_KEY` private key, sets `sub`, `aud=dealership`, `realm_access.roles`; `@BeforeEach` truncates `sales` table and resets all Resilience4j circuit breakers via `CircuitBreakerRegistry`; stub WireMock to serve JWKS from `TEST_RSA_KEY` public key at `/.well-known/jwks.json`; create `EnvironmentInitializer` (`ApplicationContextInitializer`) that injects `spring.security.oauth2.resourceserver.jwt.jwks-uri`, `spring.datasource.url`, `app.sns.endpoint-override`, `app.sns.topic-arn` from running containers — in `src/test/java/br/com/dealership/salesapi/integrated/BaseIT.java` and `EnvironmentInitializer.java`

**Checkpoint**: Application starts with `@SpringBootTest`; Flyway migration runs cleanly; `mvn test -pl . -Dtest=BaseIT` passes (containers healthy).

---

## Phase 3: User Story 1 — Register a Sale (Priority: P1) 🎯 MVP

**Goal**: A client can submit a valid sale request; the sale is persisted with 10% tax, and a self-contained JSON event is published atomically to SNS. All failure cases (invalid car status, identity mismatch, SNS failure) are correctly rejected.

**Independent Test**: Submit a valid `POST /api/v1/sales` with `AVAILABLE` car status and valid JWT → assert 201, `saleValue = listedValue × 1.10`, Location header set, SNS SQS subscription receives event payload with correct fields; then assert car-status-rejected, ownership-rejected, and SNS-failure-rolls-back paths.

### Tests — User Story 1

- [X] T020 [P] [US1] Write `SaleTest` unit tests: assert `CarSnapshotRequest` compact constructor normalizes VIN to uppercase; assert `CarSnapshotRequest` defensively copies `optionalItems`; assert `RegisterSaleRequest` compact constructor throws `IllegalArgumentException` when `carId == clientId`; assert `CarSnapshot` defensive copy of `optionalItems` — in `src/test/java/br/com/dealership/salesapi/domain/SaleTest.java`
- [X] T021 [P] [US1] Write `SnsPublisherTest` unit tests using Mockito: assert `publish()` calls `SnsClient.publish()` with correct topic ARN and JSON body; assert Resilience4j `@Retry` triggers on `SnsException`; assert `publishFallback()` throws `SnsPublishException` (not swallows) — in `src/test/java/br/com/dealership/salesapi/messaging/SnsPublisherTest.java`
- [X] T022 [US1] Write `SaleServiceTest` (Mockito) for `registerSale` scenarios: valid request → saved entity has `saleValue = listedValue × 1.10` (HALF_UP) and UUID v4 id, SNS publisher called once; car status `SOLD` → `CarNotAvailableException`; car status `UNAVAILABLE` → `CarNotAvailableException`; JWT `sub` ≠ `clientId` → `SaleOwnershipException`; `DataIntegrityViolationException` from repository → `CarAlreadySoldException`; `SnsPublishException` from publisher → propagates and triggers `@Transactional` rollback — in `src/test/java/br/com/dealership/salesapi/service/SaleServiceTest.java`
- [X] T023 [US1] Write `SaleControllerTest` (MockMvc standalone via `MockMvcBuilders.standaloneSetup(new SaleController(mockSaleService)).setControllerAdvice(new GlobalExceptionHandler()).build()` — **not** `@WebMvcTest`, which is prohibited by constitution Art. XI) for `POST /api/v1/sales`: valid body → 201 with `Response<SaleResponse>` and `Location` header; invalid body (missing `carId`, blank `vin`, negative `listedValue`) → 400 with field-level `FieldError` list; `SaleOwnershipException` → 403; `CarNotAvailableException` → 422; `CarAlreadySoldException` → 422; `SnsPublishException` → 503; no token → 401 — in `src/test/java/br/com/dealership/salesapi/controller/SaleControllerTest.java`
- [X] T024 [US1] Write `SaleRegistrationIT` extending `BaseIT`: happy path — POST valid request, assert 201, assert `saleValue` equals `listedValue × 1.10`, assert SQS message received on SNS subscription contains valid `SaleEventPayload` JSON with correct `saleId`, assert total response time ≤ 3000ms (Rest Assured `time(lessThan(3000L, MILLISECONDS))` — SC-001); car status `SOLD` → 422, no SQS message; `clientId` mismatch → 403, no SQS message; SNS endpoint down (LocalStack SNS stopped or wrong ARN) → 503, query DB assert no row persisted (rollback verified); duplicate `carId` race → 422 with `car already sold` message — in `src/test/java/br/com/dealership/salesapi/integrated/SaleRegistrationIT.java`

### Implementation — User Story 1

- [X] T025 [P] [US1] Create request records: `AddressSnapshotRequest` (all `@NotBlank` fields, `complement` optional), `ClientSnapshotRequest` (`@NotBlank`, `@Email`, `@Pattern` CPF, `@Valid` address), `CarSnapshotRequest` (`@NotBlank` fields, `@Min(1886)/@Max(2100)` year, `@Pattern` VIN, `@DecimalMin("0.01")` listedValue, `@NotNull` status, compact constructor for `List.copyOf()` and VIN `.toUpperCase()`) — in `src/main/java/br/com/dealership/salesapi/dto/request/`
- [X] T026 [US1] Create `RegisterSaleRequest` self-validating record (`@NotNull UUID carId`, `@NotNull UUID clientId`, `@Valid @NotNull ClientSnapshotRequest clientSnapshot`, `@Valid @NotNull CarSnapshotRequest carSnapshot`); compact constructor throws `IllegalArgumentException` when `carId.equals(clientId)` — in `src/main/java/br/com/dealership/salesapi/dto/request/RegisterSaleRequest.java`
- [X] T027 [P] [US1] Create nested snapshot response records: `AddressSnapshotResponse` with `static from(AddressSnapshot)`; `ClientSnapshotResponse` with `static from(ClientSnapshot)`; `CarSnapshotResponse` with `static from(CarSnapshot)`; `SaleResponse` (`@Builder` record, all sale fields, `static from(Sale)` factory that delegates to snapshot factories) — in `src/main/java/br/com/dealership/salesapi/dto/response/`
- [X] T028 [P] [US1] Create `SaleEventPayload` `@Builder` record with fields `saleId`, `carId`, `clientId` (UUID), `saleValue` (BigDecimal), `registeredAt` (Instant), `clientSnapshot` (ClientSnapshot), `carSnapshot` (CarSnapshot); implement `static SaleEventPayload from(Sale sale)` factory — in `src/main/java/br/com/dealership/salesapi/messaging/SaleEventPayload.java`
- [X] T029 [US1] Implement `SnsPublisher` `@Component`: inject `SnsClient` and `SnsProperties`; method `publish(SaleEventPayload payload)` annotated `@Retry(name="sns", fallbackMethod="publishFallback")` and `@CircuitBreaker(name="sns", fallbackMethod="publishFallback")`; serialize payload to JSON via `ObjectMapper`, call `snsClient.publish(r -> r.topicArn(snsProperties.topicArn()).message(json))`; set MDC `sns.messageId` from response; `publishFallback(SaleEventPayload, Throwable t)` must throw `new SnsPublishException(t.getMessage())` (rethrow, never swallow) — in `src/main/java/br/com/dealership/salesapi/messaging/SnsPublisher.java`
- [X] T030 [US1] Create `SaleService` `@Service` `@RequiredArgsConstructor`: implement `registerSale(RegisterSaleRequest request, JwtAuthenticationToken token)` `@Transactional`; extract `clientId` from `token.getName()` (UUID); assert `clientId.equals(request.clientId())` else throw `SaleOwnershipException`; assert `request.carSnapshot().status() == AVAILABLE` else throw `CarNotAvailableException`; compute `saleValue = request.carSnapshot().listedValue().multiply(BigDecimal.valueOf(1.10)).setScale(4, RoundingMode.HALF_UP)`; set `registeredAt = Instant.now()` on the `Sale` builder before calling `saleRepository.save()` so the timestamp is populated and available for `SaleEventPayload`; build and save `Sale` via `SaleRepository` catching `DataIntegrityViolationException` → throw `CarAlreadySoldException`; set MDC `sale.id`, `sale.clientId`, `sale.carId`, `sale.value`; call `snsPublisher.publish(SaleEventPayload.from(sale))`; return `SaleResponse.from(sale)` — in `src/main/java/br/com/dealership/salesapi/service/SaleService.java`
- [X] T031 [US1] Create `SaleController` `@RestController` `@RequestMapping("/api/v1/sales")` `@RequiredArgsConstructor` with `@Tag(name="Sales")`; implement `POST /` → `@PreAuthorize("hasRole('CLIENT')")`, `@ResponseStatus(201)`, call `saleService.registerSale(request, token)`, return `ResponseEntity` with `Location` header set to `/api/v1/sales/{id}` and body `Response.of(saleResponse)` — in `src/main/java/br/com/dealership/salesapi/controller/SaleController.java`

**Checkpoint**: `mvn test -Dtest=SaleTest,SnsPublisherTest,SaleServiceTest,SaleControllerTest` all pass; `SaleRegistrationIT` passes end-to-end.

---

## Phase 4: User Story 2 — Retrieve My Sales (Priority: P2)

**Goal**: An authenticated client can retrieve their own paginated sale history (with optional date-range filter) and look up a single sale by ID — with 403 on IDOR attempts. Single-record lookup is Redis-cached.

**Independent Test**: Seed several sales for two clients; assert ROLE_CLIENT can list own sales (paginated, date-filtered, empty), assert default page size is 20, assert ROLE_CLIENT gets 200 on own sale by ID, and 403 when accessing another client's sale ID.

### Tests — User Story 2

- [X] T032 [P] [US2] Add `SaleServiceTest` scenarios for `getClientSales()`: paginated result for own `clientId`; date-range filter returns only in-range records; empty result → empty `Page` (not exception); assert `Pageable` default size of 20; and for `getById()`: own sale → `SaleResponse`; sale belongs to other client + ROLE_CLIENT → `SaleOwnershipException`; ID not found → `SaleNotFoundException` — in `src/test/java/br/com/dealership/salesapi/service/SaleServiceTest.java`
- [X] T033 [US2] Add `SaleControllerTest` scenarios for `GET /api/v1/sales` (200 paginated, 401 no token, ROLE_STAFF → 403, ROLE_ADMIN → 403) and `GET /api/v1/sales/{id}` (200 own, 403 other client, 404 not found, 401 no token) — in `src/test/java/br/com/dealership/salesapi/controller/SaleControllerTest.java`
- [X] T034 [US2] Write `SaleRetrievalIT` extending `BaseIT`: seed 3 sales for `clientA`, 2 for `clientB`; assert `GET /api/v1/sales` with `clientA` JWT returns exactly 3 results; assert date-range filter `?from=…&to=…` returns only matching records; assert empty result for new client; assert default page size 20 applied; assert `GET /api/v1/sales/{id}` with `clientA` JWT on own sale → 200 full response; assert same endpoint with `clientB` JWT on `clientA`'s sale ID → 403; assert cache-proof IDOR: first populate the Redis cache via a valid `clientA` request, then assert a `clientB` request for the **same ID** still returns 403 (verifies C2 fix — ownership check must run regardless of cache state); assert caching: a second `clientA` request for the same ID returns an identical response (cache hit, no DB query) — in `src/test/java/br/com/dealership/salesapi/integrated/SaleRetrievalIT.java`

### Implementation — User Story 2

- [X] T035 [US2] Add `getClientSales(UUID clientId, Instant from, Instant to, Pageable pageable)` to `SaleService`: if both `from` and `to` are non-null call `saleRepository.findByClientIdAndRegisteredAtBetween()`; otherwise call `saleRepository.findByClientId()`; return `Page<SaleResponse>` mapped via `SaleResponse.from()` — in `src/main/java/br/com/dealership/salesapi/service/SaleService.java`
- [X] T036 [US2] Create `SaleCacheService` `@Service` with `@Cacheable(cacheNames = "sales", key = "#id") SaleResponse findSaleById(UUID id)` that loads from `saleRepository.findById(id)` (throws `SaleNotFoundException` if absent) and returns `SaleResponse.from(sale)` — this separate bean is required to provide a Spring AOP proxy boundary so `@Cacheable` intercepts correctly (self-invocation from within `SaleService` would bypass the proxy); then add `getById(UUID id, JwtAuthenticationToken token, boolean isStaff)` to `SaleService`: inject `SaleCacheService`, call `saleCacheService.findSaleById(id)` (cache-aware lookup), then **always** apply ownership check on the returned response: if `!isStaff` and `saleResponse.clientId()` does not equal `extractClientId(token)` → throw `SaleOwnershipException`; this guarantees 403 is enforced on every call regardless of whether the result came from cache or DB — in `src/main/java/br/com/dealership/salesapi/service/SaleCacheService.java` and `SaleService.java`
- [X] T037 [US2] Create `WebMvcConfig` `@Configuration` in `config/` with a `PageableHandlerMethodArgumentResolverCustomizer` `@Bean` that sets `maxPageSize(100)` and `fallbackPageable(PageRequest.of(0, 20))` — enforces pagination limits globally across all `Pageable` parameters (FR-011); then add `GET /api/v1/sales` and `GET /api/v1/sales/{id}` to `SaleController`: `GET /` → `@PreAuthorize("hasRole('CLIENT')")`, accept `@RequestParam(required=false) Instant from/to` and `Pageable pageable`, return `ResponseEntity<Response<Page<SaleResponse>>>`; `GET /{id}` → `@PreAuthorize("hasAnyRole('CLIENT','STAFF','ADMIN')")`, derive `isStaff` from token authorities, call `saleService.getById()` — in `src/main/java/br/com/dealership/salesapi/config/WebMvcConfig.java` and `controller/SaleController.java`

**Checkpoint**: `mvn test -Dtest=SaleServiceTest,SaleControllerTest` pass; `SaleRetrievalIT` passes; Redis cache verified in IT.

---

## Phase 5: User Story 3 — Staff Retrieves Sales Records (Priority: P3)

**Goal**: Authenticated staff and admin users can query all sales using any combination of `clientId`, `carId`, and date-range filters via a dedicated `/sales/staff` endpoint. ROLE_CLIENT is denied.

**Independent Test**: Create sales for multiple clients; assert ROLE_STAFF can list all without filters; assert each filter combination (clientId, carId, date range, combined) returns correct subset; assert ROLE_CLIENT receives 403.

### Tests — User Story 3

- [X] T038 [P] [US3] Add `SaleServiceTest` scenarios for `getStaffSales()`: no filters → all sales paginated; `clientId` filter → only that client's sales; `carId` filter → at most one result; `from`+`to` filter → date-bounded; combined `clientId`+`from`+`to` → intersection — in `src/test/java/br/com/dealership/salesapi/service/SaleServiceTest.java`
- [X] T039 [US3] Add `SaleControllerTest` scenarios for `GET /api/v1/sales/staff`: ROLE_STAFF → 200 paginated; ROLE_ADMIN → 200; ROLE_CLIENT → 403; 401 no token; assert each query-param filter is passed through to service — in `src/test/java/br/com/dealership/salesapi/controller/SaleControllerTest.java`
- [X] T040 [US3] Write `SaleSecurityIT` extending `BaseIT`: no JWT → 401 on all four endpoints; JWT with wrong `aud` → 401; ROLE_CLIENT on `GET /api/v1/sales/staff` → 403; ROLE_CLIENT on `GET /api/v1/sales/{id}` for another client's sale → 403 (not 404, IDOR protection); ROLE_STAFF on `GET /api/v1/sales/{id}` for any client → 200; ROLE_ADMIN on `GET /api/v1/sales/staff` → 200 — in `src/test/java/br/com/dealership/salesapi/integrated/SaleSecurityIT.java`

### Implementation — User Story 3

- [X] T041 [US3] Create `SaleSpecification` utility class with static factory methods: `withClientId(UUID)`, `withCarId(UUID)`, `withRegisteredAtBetween(Instant, Instant)` — each returns a `Specification<Sale>` using `CriteriaBuilder`; methods return `(root, query, cb) -> cb.conjunction()` (an always-true no-op predicate — safe to chain via `.and()`, unlike returning `null` which causes `NullPointerException` in direct composition) when the filter value is null — in `src/main/java/br/com/dealership/salesapi/repository/SaleSpecification.java`
- [X] T042 [US3] Add `getStaffSales(UUID clientId, UUID carId, Instant from, Instant to, Pageable pageable)` to `SaleService`: compose `Specification.where(withClientId(clientId)).and(withCarId(carId)).and(withRegisteredAtBetween(from, to))`; call `saleRepository.findAll(spec, pageable)`; return `Page<SaleResponse>` — in `src/main/java/br/com/dealership/salesapi/service/SaleService.java`
- [X] T043 [US3] Add `GET /api/v1/sales/staff` to `SaleController` **before** the `/{id}` mapping: `@PreAuthorize("hasAnyRole('STAFF','ADMIN')")`, accept `@RequestParam(required=false) UUID clientId`, `UUID carId`, `Instant from`, `Instant to`, `Pageable pageable`; call `saleService.getStaffSales()`; return `ResponseEntity<Response<Page<SaleResponse>>>` — in `src/main/java/br/com/dealership/salesapi/controller/SaleController.java`

**Checkpoint**: `SaleControllerTest`, `SaleServiceTest` fully pass; `SaleSecurityIT` passes; all four endpoint contract scenarios covered.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Coverage enforcement, mutation testing validation, and end-to-end quickstart verification.

- [X] T044 [P] Run `mvn verify -Pjacoco` and inspect JaCoCo report; add missing test cases to reach ≥90% line and branch coverage in `service/`, `messaging/`, `controller/`, `domain/exception/`; focus on uncovered branches in `GlobalExceptionHandler` switch and `SaleService` null-guard paths — in existing test files
- [X] T045 [P] Run PITest mutation coverage report (`mvn test-compile org.pitest:pitest-maven:mutationCoverage`); fix surviving mutants in `SaleService` tax calculation, `SaleSpecification` null guards, and `SnsPublisher` fallback logic by adding targeted assertion tests — in existing test files
- [X] T046 Run full end-to-end quickstart validation per `quickstart.md`: `docker compose up -d`, create LocalStack SNS topic + SQS subscription, `mvn verify -Pfailsafe` (all ITs green); assert `SaleRegistrationIT`, `SaleRetrievalIT`, `SaleSecurityIT` all pass; verify JaCoCo threshold enforcement does not fail the build; seed 10,000 `sales` rows directly via JDBC and assert `GET /api/v1/sales` with pagination completes in < 1000ms and `GET /api/v1/sales/staff` with each individual filter (`clientId`, `carId`, `from`/`to`) completes in < 1000ms using Rest Assured `time(lessThan(1000L, MILLISECONDS))` — satisfies SC-004 — in local environment

**Checkpoint**: `mvn verify` passes with JaCoCo ≥90%, all ITs green, no PITest surviving mutants above threshold.

---

## Dependencies & Execution Order

### Phase Dependencies

```
Phase 1 (Setup)
    └─► Phase 2 (Foundational)   ← BLOCKS all user stories
            ├─► Phase 3 (US1 — P1)   ← MVP: can deploy after this
            ├─► Phase 4 (US2 — P2)   ← depends on Phase 2 + US1 sale records existing
            └─► Phase 5 (US3 — P3)   ← depends on Phase 2; independent of US2
                        └─► Phase 6 (Polish)
```

### User Story Dependencies

| Story | Can Start After | Depends On |
|-------|-----------------|------------|
| US1 (P1) | Phase 2 complete | Nothing else |
| US2 (P2) | Phase 2 complete | US1 sale records must exist for ITs; service layer independent |
| US3 (P3) | Phase 2 complete | Reuses `SaleService` and `SaleController` from US1/US2 but adds independently |

### Within Each Story (required order)

```
Record + domain validation tests
    └─► Domain/DTO implementation
            └─► Service unit tests
                    └─► Service implementation
                            └─► Controller unit tests
                                    └─► Controller implementation
                                            └─► Integration test
```

### Parallel Opportunities Per Story

**Phase 2 parallelizable tasks** (all independent files):
```
T003 (SQL migration)
T004 (CarStatus enum)          ─┐
T005 (AddressSnapshot)          │ all in parallel
T006 (ClientSnapshot)           │
T007 (CarSnapshot)             ─┘
    └─► T008 (Sale entity)
            └─► T009 (SaleRepository)
T010 (exception hierarchy)
T011 (Response records)        ─┐ in parallel
T013 (SecurityConfig)           │
T014 (SnsProperties)            │
T016 (RedisConfig)              │
T017 (OpenApiConfig)            │
T018 (RequestLoggingFilter)     │
T019 (BaseIT)                  ─┘
    └─► T012 (GlobalExceptionHandler) ← needs T010 + T011
    └─► T015 (SnsConfig)              ← needs T014
```

**Phase 3 (US1) parallelizable**:
```
T020 (SaleTest)                ─┐
T021 (SnsPublisherTest)         │ parallel test prep
                               ─┘
T025 (request records)         ─┐
T027 (response records)         │ parallel DTO creation
T028 (SaleEventPayload)        ─┘
    └─► T026 (RegisterSaleRequest)   ← needs T025
    └─► T029 (SnsPublisher)          ← needs T028
    └─► T030 (SaleService)           ← needs T026, T029, T027
    └─► T022 (SaleServiceTest)       ← write after T030
    └─► T031 (SaleController)        ← needs T030
    └─► T023 (SaleControllerTest)    ← write after T031
    └─► T024 (SaleRegistrationIT)    ← write last, needs all
```

---

## Implementation Strategy

### Suggested MVP Scope

Deliver **Phase 1 + Phase 2 + Phase 3 (US1)** as MVP:
- Single `POST /api/v1/sales` endpoint fully operational
- Atomic persist + SNS publish
- All failure paths handled (422, 403, 503, 400, 401)
- `SaleRegistrationIT` green

This MVP satisfies **SC-001** and **SC-002** and unblocks all downstream SNS consumers.

### Incremental Delivery Order

```
Sprint 1:  Phase 1 + Phase 2 → Foundation ready
Sprint 2:  Phase 3 (US1)    → MVP deployable
Sprint 3:  Phase 4 (US2)    → Client read path
Sprint 4:  Phase 5 (US3)    → Staff operational visibility
Sprint 5:  Phase 6 (Polish) → Coverage + validation
```

### Key Ordering Rules

1. `GET /api/v1/sales/staff` **must be declared before** `GET /api/v1/sales/{id}` in `SaleController` — literal path takes precedence in Spring MVC
2. `@Cacheable` applies **only** to `getById()` — list endpoints must NOT be annotated
3. `SnsPublisher` fallback **must rethrow** `SnsPublishException` — never silently swallow to ensure `@Transactional` rollback
4. `aspectj.retry-aspect-order=2` and `circuit-breaker-aspect-order=3` — Retry wraps CircuitBreaker → `Retry(CB(call))`
5. `registeredAt` should be set in service via `Instant.now()` (not rely on DB default) so the entity has the value before SNS publish

---

## Summary

| Metric | Value |
|--------|-------|
| Total tasks | 46 |
| Phase 1 (Setup) | 2 |
| Phase 2 (Foundational) | 17 |
| Phase 3 (US1) | 12 |
| Phase 4 (US2) | 6 |
| Phase 5 (US3) | 6 |
| Phase 6 (Polish) | 3 |
| Parallelizable tasks [P] | 22 |
| Integration test files | 3 (SaleRegistrationIT, SaleRetrievalIT, SaleSecurityIT) |
| Unit test files | 4 (SaleTest, SnsPublisherTest, SaleServiceTest, SaleControllerTest) |
| MVP scope | Phase 1 + 2 + 3 (US1) — 31 tasks |
