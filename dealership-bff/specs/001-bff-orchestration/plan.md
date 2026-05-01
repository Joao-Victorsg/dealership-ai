# Implementation Plan: BFF Orchestration Service

**Branch**: `master` | **Date**: 2026-04-26 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/001-bff-orchestration/spec.md`

## Summary

The dealership BFF is a Spring Boot 4.0.6 / Java 25 REST API that acts as the sole entry point for the dealership website into the platform's backend services. It aggregates and transforms data from the Car API, Client API, Sales API, and Keycloak; orchestrates authentication via the Keycloak ROPC flow; enforces security at the edge (input sanitization, header stripping, JWT propagation); applies a full Resilience4j chain (Retry → CircuitBreaker → RateLimiter → TimeLimiter → Bulkhead) per downstream service; caches public car inventory in Redis (TTL 5 min); and wraps every response — success or error — in a standardized JSON envelope.

All downstream service structures are mapped to BFF-owned DTOs via Feign client type hierarchies. No downstream model is ever forwarded directly to the frontend.

## Technical Context

**Language/Version**: Java 25
**Primary Dependencies**: Spring Boot 4.0.6, Spring Cloud BOM 2025.1.0, Spring Cloud OpenFeign 5.0.x, resilience4j-spring-boot4 (latest stable), springdoc-openapi-starter-webmvc-ui 3.0.2, Spring Security OAuth2 Resource Server, Spring Data Redis, Spring Boot Actuator, Jackson (via Boot), Lombok
**Storage**: Redis (ElastiCache-compatible) for car inventory cache (TTL 5 min) only. No database.
**Testing**: JUnit 5, Mockito, Instancio 5.3.0, PITest 1.19.1 + pitest-junit5-plugin 1.2.3, WireMock (`org.wiremock.integrations:wiremock-spring-boot` latest compatible with Spring Boot 4.x), Testcontainers (Redis), RestAssured 6.0.0, JaCoCo 0.8.13
**Target Platform**: Linux server (containerized, Spring Boot fat JAR with virtual threads)
**Project Type**: web-service (REST API / BFF)
**Performance Goals**: p99 ≤ 500 ms; p50 ≤ 300 ms for all composed endpoints
**Constraints**: No persistent state; all state lives in downstream services; Redis for cache only; refresh token in HttpOnly cookie only; no New Relic SDK in application code
**Scale/Scope**: Serves dealership website frontend; 4 downstream clients (Car API, Client API, Sales API, Keycloak)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Article | Gate | Status |
|---------|------|--------|
| I — Purpose | BFF must never expose downstream structures directly to the client | ✅ All downstream types mapped to BFF-owned DTOs via Feign client DTO hierarchy; Feign types never reach controllers |
| II — Library Versions | Spring Boot ≥ 4.0.5, Spring Cloud BOM 2025.1.0, PITest 1.19.1, pitest-junit5-plugin 1.2.3 | ✅ `pom.xml` already uses Spring Boot 4.0.6 (newer stable than 4.0.5 — compliant per Article II) |
| III — Envelope | Every response enveloped; global `ControllerAdvice`; no raw Spring error format | ✅ `ApiResponse<T>` and `ApiErrorResponse` as immutable records with static `of`; `GlobalExceptionHandler` handles all unhandled exceptions |
| IV — Security | Input sanitization before service layer; HttpOnly refresh cookie; header stripping; JWT propagation to downstream | ✅ Dedicated `InputSanitizationFilter`; STATELESS Spring Security; Feign `RequestInterceptor` propagates JWT and strips client-supplied internal headers |
| V — Parallelism | Independent downstream calls in parallel; fail-fast | ✅ Purchase assembly uses `CompletableFuture.allOf` with immediate cancellation on any failure |
| VI — Resilience | Retry→CB→RL→TL→BH chain; per-service instances; no hardcoded config; Sales POST never retried; semaphore bulkhead on Sales | ✅ Resilience4j aspect orders configured externally; `PurchaseService.registerSale` explicitly excluded from retry; `BulkheadConfig(type=SEMAPHORE)` on all Sales calls |
| VII — Immutability | `final` everywhere; records for all DTOs; builder + `of` factory; unmodifiable collections | ✅ Enforced by coding conventions; review gate during implementation |
| VIII — Caching | Redis 5-min TTL for inventory; no user-specific data cached; cache eviction on successful sale | ✅ `@Cacheable` on inventory methods; `@CacheEvict` in `PurchaseService` post-sale |
| IX — Testing | ≥90% line+branch coverage; PITest 1.19.1 + mutation ≥90%; integration tests excluded from PITest; `auto_threads`; WireMock per client | ✅ JaCoCo and PITest config follow Car API pattern, updated to required versions |
| X — Observability | New Relic via Java Agent only; Actuator health/readiness/liveness; requestId in every log entry and envelope | ✅ No New Relic SDK; `RequestIdFilter` populates MDC and request-scoped bean; `RequestLoggingFilter` logs sanitized method/path/status/latency/subject |
| XI — API Design | RESTful; all responses enveloped; springdoc-openapi; all endpoints documented | ✅ springdoc-openapi-starter-webmvc-ui 3.0.2 (identical to Car API); every endpoint annotated with `@Operation` and envelope schema |

**Constitution Check: ALL GATES PASS** ✅

---

## Project Structure

### Documentation (this feature)

```text
specs/001-bff-orchestration/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   ├── auth.md
│   ├── inventory.md
│   ├── profile.md
│   └── purchases.md
└── tasks.md             # Phase 2 output (NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
src/
└── main/
    ├── java/br/com/dealership/dealershibff/
    │   ├── DealershiBffApplication.java          # @SpringBootApplication @EnableFeignClients
    │   ├── config/
    │   │   ├── AsyncConfig.java                  # Virtual threads executor for CompletableFuture
    │   │   ├── FeignConfig.java                  # Global Feign config (Jackson decoder, error handling)
    │   │   ├── GlobalExceptionHandler.java        # @RestControllerAdvice — all unhandled → ApiErrorResponse
    │   │   ├── OpenApiConfig.java                # springdoc: security scheme, envelope schemas
    │   │   ├── RedisConfig.java                  # Cache manager, TTL, key serialization
    │   │   ├── ResilienceConfig.java             # Aspect order constants (Retry=1…Bulkhead=5)
    │   │   └── SecurityConfig.java               # STATELESS, OAuth2 Resource Server, public paths
    │   ├── controller/
    │   │   ├── AuthController.java               # POST /api/v1/auth/register, /login, /logout
    │   │   ├── InventoryController.java          # GET /api/v1/inventory, /inventory/search, /inventory/{id}
    │   │   ├── ProfileController.java            # GET/PATCH /api/v1/profile
    │   │   └── PurchaseController.java           # POST/GET /api/v1/purchases
    │   ├── domain/
    │   │   ├── enums/
    │   │   │   └── ErrorCode.java                # CAR_NOT_AVAILABLE, VALIDATION_ERROR, etc.
    │   │   └── exception/
    │   │       ├── BffException.java             # Base unchecked exception with ErrorCode
    │   │       ├── CarNotAvailableException.java  # CAR_NOT_AVAILABLE
    │   │       ├── DownstreamServiceException.java # DOWNSTREAM_UNAVAILABLE / circuit open / timeout
    │   │       └── RegistrationException.java     # Registration flow failures
    │   ├── dto/
    │   │   ├── request/
    │   │   │   ├── InventoryFilterRequest.java    # Filter + sort + pagination params
    │   │   │   ├── LoginRequest.java              # email, password
    │   │   │   ├── PurchaseRequest.java           # carId
    │   │   │   ├── RegisterRequest.java           # email, password, firstName, lastName, cpf, phone, cep
    │   │   │   └── UpdateProfileRequest.java      # firstName, lastName, phone, cep (no CPF)
    │   │   └── response/
    │   │       ├── ApiResponse.java               # record: data, meta — static of(T data, ResponseMeta meta)
    │   │       ├── ApiErrorResponse.java          # record: error, meta — static of(ErrorBody, ResponseMeta)
    │   │       ├── ErrorBody.java                 # record: code, message, details
    │   │       ├── ErrorDetail.java               # record: field, reason
    │   │       ├── ResponseMeta.java              # record: timestamp, requestId[, pagination]
    │   │       ├── VehicleResponse.java           # BFF-owned car DTO (mapped from CarApiCarResponse)
    │   │       ├── ProfileResponse.java           # BFF-owned client DTO (mapped from ClientApiClientResponse)
    │   │       ├── PurchaseResponse.java          # BFF-owned sale DTO (mapped from SalesApiSaleResponse)
    │   │       └── TokenResponse.java             # accessToken only (refresh in cookie)
    │   ├── feign/
    │   │   ├── car/
    │   │   │   ├── CarApiClient.java              # @FeignClient("car-api")
    │   │   │   ├── CarApiErrorDecoder.java        # 404→CarNotFound, 503→DownstreamService, etc.
    │   │   │   └── dto/                           # CarApiCarResponse, CarApiPageResponse, CarApiFilterParams
    │   │   ├── client/
    │   │   │   ├── ClientApiClient.java           # @FeignClient("client-api")
    │   │   │   ├── ClientApiErrorDecoder.java     # 422→DuplicateIdentity, 403→OwnershipViolation
    │   │   │   └── dto/                           # ClientApiClientResponse, ClientApiCreateRequest, ClientApiUpdateRequest
    │   │   ├── sales/
    │   │   │   ├── SalesApiClient.java            # @FeignClient("sales-api")
    │   │   │   ├── SalesApiErrorDecoder.java      # 409→CarNotAvailable, 503→DownstreamService
    │   │   │   └── dto/                           # SalesApiRegisterRequest, SalesApiSaleResponse, snapshot DTOs
    │   │   └── keycloak/
    │   │       ├── KeycloakClient.java            # @FeignClient("keycloak") — token exchange, user mgmt
    │   │       ├── KeycloakErrorDecoder.java      # 401→AuthenticationRequired, 409→DuplicateIdentity
    │   │       └── dto/                           # KeycloakTokenResponse, KeycloakCreateUserRequest
    │   ├── service/
    │   │   ├── AuthService.java                   # login, logout, register (Keycloak + Client API), refresh
    │   │   ├── InventoryService.java              # list, search, getById (Car API + Redis cache)
    │   │   ├── ProfileService.java                # getProfile, updateProfile (Client API)
    │   │   └── PurchaseService.java               # purchase (parallel Car+Client → Sales), history
    │   └── web/
    │       ├── InputSanitizationFilter.java       # OncePerRequestFilter — validates CPF/CEP/phone; sanitizes search
    │       ├── RequestIdFilter.java               # OncePerRequestFilter — generates/extracts requestId → MDC
    │       └── RequestLoggingFilter.java          # OncePerRequestFilter — logs method/path/status/latency/subject
    └── resources/
        ├── application.properties                 # Main config (env var placeholders)
        └── application-test.properties            # Integration test overrides

src/
└── test/
    ├── java/br/com/dealership/dealershibff/       # Unit tests (Surefire)
    │   ├── controller/                            # MockMvc standaloneSetup per controller
    │   ├── service/                               # @ExtendWith(MockitoExtension.class)
    │   └── web/                                   # Filter unit tests
    └── java/integrated/                           # Integration tests (Failsafe; excluded from Surefire)
        ├── BaseIT.java                            # @SpringBootTest(webEnvironment=RANDOM_PORT)
        ├── EnvironmentInitializer.java            # Testcontainers: Redis + 4 WireMock servers
        ├── auth/
        │   ├── LoginIT.java
        │   ├── LogoutIT.java
        │   └── RegisterIT.java
        ├── inventory/
        │   ├── InventoryListIT.java
        │   └── InventorySearchIT.java
        ├── profile/
        │   └── ProfileIT.java
        ├── purchase/
        │   ├── PurchaseIT.java
        │   └── PurchaseHistoryIT.java
        ├── resilience/
        │   ├── CircuitBreakerIT.java
        │   ├── RetryExhaustionIT.java
        │   ├── RateLimiterIT.java
        │   └── BulkheadIT.java
        ├── security/
        │   ├── UnauthenticatedAccessIT.java
        │   ├── ForbiddenRoleIT.java
        │   └── InputSanitizationIT.java
        └── utils/
            └── JwtTestUtils.java
```

**Structure Decision**: Single Spring Boot project. Maven multi-module is not warranted — the BFF is a single deployable unit with no independently reusable libraries. Source layout matches the Car API and Client API patterns established in the platform.

## Complexity Tracking

> No Constitution Check violations. No complexity justification required.
