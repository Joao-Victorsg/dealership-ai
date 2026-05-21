# Implementation Plan: Sales API

**Branch**: `001-sales-api` | **Date**: 2026-04-19 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `specs/001-sales-api/spec.md`

## Summary

The Sales API is a Spring Boot 4.0.5 / Java 25 REST microservice that owns all
sale records in the dealership platform. It accepts sale registrations (with
pre-assembled client and car snapshots), applies a 10% domain-level tax,
persists the record to Aurora PostgreSQL with JSONB snapshots, and publishes a
self-contained JSON event to SNS — all as a single `@Transactional` atomic outcome.

Read endpoints (paginated list + single GET) support client-owned access and
staff/admin unrestricted access. Individual sale lookups are served through
Redis-backed caching (24h TTL). Multi-filter list endpoints are not cached.

Resilience is provided by `@Retry` + `@CircuitBreaker` on the SNS call, with the
AWS SDK's `apiCallTimeout(2s)` bounding the wall-clock duration of each attempt.
On failure, the fallback rethrows — preventing the sale from committing.

## Technical Context

**Language/Version**: Java 25 / Spring Boot 4.0.5  
**Primary Dependencies**: Spring Security OAuth2 Resource Server, Spring Data JPA
(Hibernate 7), Flyway, Resilience4j 2.3.0, AWS SDK v2 SNS 2.25.60, Spring Data Redis,
springdoc-openapi 3.0.2, Lombok  
**Storage**: Aurora PostgreSQL — JPA / Hibernate 7; Flyway migrations; JSONB columns
for client and car snapshots  
**Testing**: JUnit 5, Mockito, Instancio 5.3.0, Rest Assured 6.0.0, Testcontainers
1.20.4 (`postgres:17-alpine`, WireMock `wiremock/wiremock:3.13.0`, LocalStack
`localstack/localstack:4.4`), Nimbus JOSE+JWT 9.48, JaCoCo 0.8.13, PITest 1.17.0  
**Target Platform**: Linux server — AWS ECS + Aurora PostgreSQL + ElastiCache Redis + SNS  
**Project Type**: REST web-service (microservice)  
**Performance Goals**: <3 s sale registration end-to-end; <1 s list queries ≤10K records  
**Constraints**: Virtual threads; no OSIV; sealed exceptions; append-only entity;
JSONB snapshots; `final var`; self-validating records; JWT `aud` validation  
**Scale/Scope**: Single microservice; 3 user stories; 4 endpoints

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Article | Requirement | Gate Status |
|---------|-------------|-------------|
| I — Purpose | Sales API is sole owner of sale records; SNS publish triggers post-sale workflow | ✅ PASS |
| II — Identity Boundary | OAuth2 Resource Server; JWKS fetched at startup; `aud = dealership` enforced in non-local | ✅ PASS |
| III — Data Ownership | Private DB; JSONB snapshots; no calls to Car/Client API; append-only | ✅ PASS |
| IV — Business Rules | 10% tax isolated in domain; `Available` status enforced at service layer; both IDs required | ✅ PASS |
| V — Event Publishing | SNS publish atomically tied to persistence via `@Transactional` rollback; rethrow on failure; JSON payload | ✅ PASS |
| VI — Car Status Update | No Car API call; downstream Lambda handles status update via SQS | ✅ PASS |
| VII — Authorization | `@EnableMethodSecurity` + `@PreAuthorize`; ownership at service layer; 403 for IDOR | ✅ PASS |
| VIII — Immutability | `@Builder` + no `@Setter`; records + `List.copyOf()`; JSONB; sealed `SalesApiException` | ✅ PASS |
| IX — API Design | `Response<T>` envelope; 400/422/401/403 semantics; springdoc; pagination 20/100; `non_null` | ✅ PASS |
| X — Resilience | `@Retry` + `@CircuitBreaker` (aspect order enforced); AWS SDK `apiCallTimeout(2s)`; `SnsProperties` record; virtual threads | ✅ PASS |
| XI — Testing | MockMvc standalone; JaCoCo ≥90%; PITest ≥90%; `BaseIT`; WireMock container JWKS; LocalStack SNS assertion | ✅ PASS |
| XII — Observability | New Relic agent (javaagent); Actuator health/readiness; `RequestLoggingFilter`; MDC per registration and SNS event | ✅ PASS |
| XIII — Conventions | `final var`; self-validating records; `LoggerFactory.getLogger`; package layout; JPA settings; OpenAPI annotations | ✅ PASS |

**All 13 gates pass. No violations to justify.**

## Project Structure

### Documentation (this feature)

```text
specs/001-sales-api/
├── plan.md              ← This file
├── research.md          ← Phase 0 output
├── data-model.md        ← Phase 1 output
├── quickstart.md        ← Phase 1 output
├── contracts/
│   ├── POST_sales.md
│   ├── GET_sales.md
│   ├── GET_sales_id.md
│   └── GET_sales_staff.md
└── tasks.md             ← Phase 2 output (/speckit.tasks — NOT created here)
```

### Source Code (repository root)

```text
src/
├── main/
│   ├── java/br/com/dealership/salesapi/
│   │   ├── config/
│   │   │   ├── SecurityConfig.java              ← OAuth2 resource server, role converter
│   │   │   ├── OpenApiConfig.java               ← OpenAPI bean, bearer auth scheme
│   │   │   ├── SnsConfig.java                   ← SnsClient bean (AWS SDK v2)
│   │   │   ├── RedisConfig.java                 ← RedisCacheManager, "sales" cache 24h TTL
│   │   │   └── GlobalExceptionHandler.java      ← Sealed exception → HTTP status mapping
│   │   ├── controller/
│   │   │   └── SaleController.java              ← 4 endpoints
│   │   ├── domain/
│   │   │   ├── entity/
│   │   │   │   ├── Sale.java                    ← JPA entity (@Builder, no @Setter)
│   │   │   │   ├── ClientSnapshot.java          ← JSONB value object (record)
│   │   │   │   ├── AddressSnapshot.java         ← Nested address record
│   │   │   │   ├── CarSnapshot.java             ← JSONB value object (record)
│   │   │   │   └── CarStatus.java               ← Enum: AVAILABLE, SOLD, UNAVAILABLE
│   │   │   └── exception/
│   │   │       ├── SalesApiException.java       ← sealed base
│   │   │       ├── CarNotAvailableException.java    ← final → 422
│   │   │       ├── SaleOwnershipException.java      ← final → 403
│   │   │       ├── CarAlreadySoldException.java     ← final → 422
│   │   │       ├── SaleNotFoundException.java       ← final → 404
│   │   │       └── SnsPublishException.java         ← final → 503
│   │   ├── dto/
│   │   │   ├── request/
│   │   │   │   ├── RegisterSaleRequest.java     ← self-validating record
│   │   │   │   ├── ClientSnapshotRequest.java   ← nested record
│   │   │   │   ├── AddressSnapshotRequest.java  ← nested record
│   │   │   │   └── CarSnapshotRequest.java      ← nested record (VIN normalization in compact ctor)
│   │   │   └── response/
│   │   │       ├── Response.java                ← Response<T>(T data) record
│   │   │       ├── SaleResponse.java            ← record + static from(Sale)
│   │   │       ├── ClientSnapshotResponse.java  ← record
│   │   │       ├── AddressSnapshotResponse.java ← record
│   │   │       ├── CarSnapshotResponse.java     ← record
│   │   │       ├── ErrorResponse.java           ← @Builder record
│   │   │       └── FieldError.java              ← record + static of(field, message)
│   │   ├── messaging/
│   │   │   ├── SnsPublisher.java                ← @Retry + @CircuitBreaker; publishes JSON
│   │   │   ├── SaleEventPayload.java            ← @Builder record, static from(Sale)
│   │   │   └── SnsProperties.java               ← @ConfigurationProperties record
│   │   ├── repository/
│   │   │   └── SaleRepository.java              ← JpaRepository + Specification<Sale>
│   │   ├── service/
│   │   │   └── SaleService.java                 ← @Transactional; tax; status; ownership; cache
│   │   └── web/
│   │       └── RequestLoggingFilter.java        ← OncePerRequestFilter; method/path/status/latency
│   └── resources/
│       ├── application.properties
│       └── db/migration/
│           └── V1__create_sales_table.sql
└── test/
    └── java/br/com/dealership/salesapi/
        ├── controller/
        │   └── SaleControllerTest.java          ← MockMvc standalone
        ├── service/
        │   └── SaleServiceTest.java             ← tax, status, ownership rules
        ├── messaging/
        │   └── SnsPublisherTest.java            ← publish + fallback
        ├── domain/
        │   └── SaleTest.java                    ← record compact constructor validation
        └── integrated/
            ├── BaseIT.java                      ← containers, JWT helper, DB truncate, CB reset
            ├── EnvironmentInitializer.java      ← injects container URLs
            ├── SaleRegistrationIT.java          ← happy path, SNS assertion, rollback
            ├── SaleRetrievalIT.java             ← list, single GET, staff filters
            └── SaleSecurityIT.java              ← 401, 403, IDOR, audience
```

**Structure Decision**: Standard single-project Spring Boot Maven layout. Integration
tests are in `src/test/java/.../integrated/` (separate sub-package) to allow
Maven Surefire exclusion and Failsafe execution. No module split.
│   │   │       ├── Response.java                ← Response<T>(T data) record
│   │   │       ├── SaleResponse.java            ← record + static from(Sale)
│   │   │       ├── ClientSnapshotResponse.java  ← record
│   │   │       ├── AddressSnapshotResponse.java ← record
│   │   │       ├── CarSnapshotResponse.java     ← record
│   │   │       ├── ErrorResponse.java           ← @Builder record
│   │   │       └── FieldError.java              ← record + static of(field, message)
│   │   ├── messaging/
│   │   │   ├── SnsPublisher.java                ← @Retry + @CircuitBreaker; publishes JSON
│   │   │   ├── SaleEventPayload.java            ← @Builder record, static from(Sale)
│   │   │   └── SnsProperties.java               ← @ConfigurationProperties record
│   │   ├── repository/
│   │   │   └── SaleRepository.java              ← JpaRepository + Specification<Sale>
│   │   ├── service/
│   │   │   └── SaleService.java                 ← @Transactional; tax; status; ownership; cache
│   │   └── web/
│   │       └── RequestLoggingFilter.java        ← OncePerRequestFilter; method/path/status/latency
│   └── resources/
│       ├── application.properties
│       └── db/migration/
│           └── V1__create_sales_table.sql
└── test/
    └── java/br/com/dealership/salesapi/
        ├── controller/
        │   └── SaleControllerTest.java          ← MockMvc standalone
        ├── service/
        │   └── SaleServiceTest.java             ← tax, status, ownership rules
        ├── messaging/
        │   └── SnsPublisherTest.java            ← publish + fallback
        ├── domain/
        │   └── SaleTest.java                    ← record compact constructor validation
        └── integrated/
            ├── BaseIT.java                      ← containers, JWT helper, DB truncate, CB reset
            ├── EnvironmentInitializer.java      ← injects container URLs
            ├── SaleRegistrationIT.java          ← happy path, SNS assertion, rollback
            ├── SaleRetrievalIT.java             ← list, single GET, staff filters
            └── SaleSecurityIT.java              ← 401, 403, IDOR, audience
```

**Structure Decision**: Standard single-project Spring Boot Maven layout. Integration
tests are in `src/test/java/.../integrated/` (separate sub-package) to allow
Maven Surefire exclusion and Failsafe execution. No module split.

---

## Implementation Strategy by Layer

### 1. Database Layer

**Flyway migration** (`V1__create_sales_table.sql`):
- `sales` table with UUID PK, JSONB client/car snapshots
- UNIQUE constraint on `car_id` (FR-017)
- CHECK constraint `sale_value > 0`
- Indexes: `client_id`, `registered_at`, composite `(client_id, registered_at)`
- Full schema in [data-model.md](data-model.md)

**JPA Settings**:
- `spring.jpa.open-in-view=false`
- `spring.jpa.hibernate.ddl-auto=validate`
- `spring.jpa.properties.hibernate.jdbc.time_zone=UTC`

### 2. Security Layer

`SecurityConfig`:
- `@EnableWebSecurity` + `@EnableMethodSecurity`
- Stateless session; CSRF disabled
- All requests require authentication (no `permitAll()` except Actuator health probes)
- `JwtAuthenticationConverter` with custom `KeycloakRolesConverter` extracting from `realm_access.roles`, prefixing `ROLE_`
- Audience validation via `spring.security.oauth2.resourceserver.jwt.audiences=dealership`

### 3. Controller Layer

`SaleController` — 4 endpoints with `@PreAuthorize`:
```
POST   /api/v1/sales         → hasRole('CLIENT')
GET    /api/v1/sales         → hasRole('CLIENT')
GET    /api/v1/sales/staff   → hasAnyRole('STAFF','ADMIN')   ← must be declared before /{id}
GET    /api/v1/sales/{id}    → hasAnyRole('CLIENT','STAFF','ADMIN')
```
All return `ResponseEntity<Response<T>>`. Location header set to new resource URI on 201.

### 4. Service Layer

`SaleService.registerSale(RegisterSaleRequest, JwtAuthenticationToken)` — `@Transactional`:
1. Extract `clientId` from JWT `sub`; compare to `request.clientId()` → `SaleOwnershipException` (403)
2. Validate `request.carSnapshot().status() == AVAILABLE` → `CarNotAvailableException` (422)
3. `saleValue = carSnapshot.listedValue() × 1.10` (rounded HALF_UP, 4 decimal places)
4. `saleRepository.save(sale)` — catch `DataIntegrityViolationException` → `CarAlreadySoldException` (422)
5. Set MDC: `saleId`, `carId`, `clientId`, `saleValue`
6. `snsPublisher.publish(SaleEventPayload.from(sale))` — `SnsPublishException` triggers rollback
7. Return `SaleResponse.from(sale)`

`SaleService.getById(UUID id, UUID requestingClientId, boolean isStaff)` — `@Cacheable("sales")`:
- Load from cache or DB
- If ROLE_CLIENT and `sale.clientId != requestingClientId` → `SaleOwnershipException` (403)

### 5. SNS Publisher

`SnsPublisher.publish(SaleEventPayload)`:
- Annotated `@Retry(name="sns", fallbackMethod="publishFallback")` + `@CircuitBreaker(name="sns", fallbackMethod="publishFallback")`
- Aspect ordering: `retry-aspect-order=2 < circuit-breaker-aspect-order=3` → `Retry(CB(call))`
- Each attempt bounded by AWS SDK `apiCallTimeout(2s)` + `apiCallAttemptTimeout(1s)`
- `publishFallback(SaleEventPayload, Throwable)` → throws `SnsPublishException` (rethrow, not silence)

### 6. Integration Test Pattern

`BaseIT` (static containers, start-once-per-JVM):
- `PostgreSQLContainer` (`postgres:17-alpine`)
- `WireMockContainer` (`wiremock/wiremock:3.13.0`) — serves JWKS
- `LocalStackContainer` (`localstack/localstack:4.4`) — SNS + SQS
- Static `RSAKey TEST_RSA_KEY` (Nimbus RSA-2048 keypair generated once)
- `buildToken(UUID subject, String... roles)` helper signs JWTs with private key
- `@BeforeEach`: `TRUNCATE TABLE sales; circuitBreakerRegistry.reset()`
- `@BeforeAll` (in `SaleRegistrationIT`): create SNS topic + SQS queue + subscription

`EnvironmentInitializer` injects before Spring context loads:
- `spring.security.oauth2.resourceserver.jwt.jwks-uri` → WireMock JWKS URL
- `spring.datasource.url` → PostgreSQL container JDBC URL
- `app.sns.endpoint-override` → LocalStack URL
- `app.sns.topic-arn` → created topic ARN

---

## Post-Design Constitution Re-Check

All 13 articles satisfied after Phase 1 design:

| Check | Verification |
|-------|-------------|
| JSONB snapshots | `@JdbcTypeCode(SqlTypes.JSON)` on `ClientSnapshot` + `CarSnapshot` records |
| Sealed exceptions | `SalesApiException sealed permits` 5 final leaf types; exhaustive switch in handler |
| Records + `List.copyOf()` | All DTOs are records; `CarSnapshotRequest` + `CarSnapshot` defensively copy `optionalItems` |
| No `@Setter` | `Sale` uses `@AllArgsConstructor(PRIVATE)` + `@Builder`; no mutation path |
| Self-validating records | VIN normalized to uppercase in `CarSnapshotRequest` compact ctor; cross-field check in `RegisterSaleRequest` |
| `/sales/staff` before `/{id}` | Route ordering in Spring MVC — literal paths match before path variables |
| No cached lists | Only `getById()` has `@Cacheable`; list methods have no cache annotation |
| Transactional atomicity | Persist-then-publish; `@Transactional` + `RuntimeException` rollback; no early `flush()` |
| Aspect ordering | `retry-aspect-order=2 < circuit-breaker-aspect-order=3` → `Retry(CB(call))` |

**No violations. Plan is complete and ready for `/speckit.tasks`.**

---

## Dependencies Added (Summary)

See [quickstart.md](quickstart.md) §1 for full XML snippets.

| Dependency | Version | Reason |
|---|---|---|
| `spring-boot-starter-oauth2-resource-server` | Boot BOM | JWT validation |
| `spring-boot-starter-aop` | Boot BOM | Required by Resilience4j annotations |
| `spring-boot-starter-data-redis` | Boot BOM | Redis cache |
| `resilience4j-spring-boot3` | 2.3.0 | @Retry + @CircuitBreaker |
| `software.amazon.awssdk:sns` | 2.25.60 (via BOM) | SNS publish |
| `software.amazon.awssdk:sqs` (test) | 2.25.60 | LocalStack SNS assertion via SQS |
| `instancio-junit` (test) | 5.3.0 | Test fixture generation |
| `rest-assured` (test) | 6.0.0 | IT HTTP layer |
| `wiremock-testcontainers-module` (test) | 1.0-alpha-14 | JWKS endpoint container |
| `testcontainers:localstack` (test) | TC BOM | SNS |
| `nimbus-jose-jwt` (test) | 9.48 | RSA JWT generation |
| JaCoCo plugin | 0.8.13 | Coverage enforcement ≥90% |
| PITest plugin | 1.17.0 | Mutation testing ≥90% |
| Maven Failsafe plugin | inherited | IT execution |
