# Research: BFF Orchestration Service

**Phase**: 0 — Resolve all NEEDS CLARIFICATION from Technical Context
**Date**: 2026-04-26

---

## R-01: Spring Cloud OpenFeign 5.0.x with Spring Boot 4.0.x

**Decision**: Use `spring-cloud-starter-openfeign` managed by Spring Cloud BOM 2025.1.0. No version declared manually.

**Rationale**: Spring Cloud BOM 2025.1.0 targets Spring Boot 4.0.x and manages all Spring Cloud OpenFeign 5.0.x artifacts. The BOM version aligns with the Boot version family, preventing classpath conflicts.

**Key configuration facts**:
- Enable with `@EnableFeignClients` on the main application class.
- Per-client timeouts externalized via `spring.cloud.openfeign.client.config.<clientName>.connect-timeout` and `read-timeout` (milliseconds). No client may omit these; defaults (`-1` / `0`) are constitutionally prohibited.
- Custom `ErrorDecoder` registered per client by passing `configuration = XxxFeignConfig.class` on the `@FeignClient` annotation.
- `spring.cloud.openfeign.client.config.<clientName>.error-decoder` or `@Bean` inside the per-client `@Configuration` class.
- Request interceptors (for JWT propagation and header stripping) registered as `@Bean RequestInterceptor` inside the per-client configuration or globally.

**Alternatives considered**: `RestClient` (Spring 6.1+). Rejected — lacks built-in Resilience4j integration, Spring Cloud Circuit Breaker fallback support, and the declarative interface alignment that reduces boilerplate across four clients.

---

## R-02: Resilience4j resilience4j-spring-boot4 — Chain Order and Configuration

**Decision**: Use `io.github.resilience4j:resilience4j-spring-boot4` (latest stable, managed by Spring Cloud BOM 2025.1.0). Configure aspect order to enforce Retry → CircuitBreaker → RateLimiter → TimeLimiter → Bulkhead chain.

**Rationale**: resilience4j-spring-boot4 is the Boot 4.x-compatible module replacing `resilience4j-spring-boot3`. It registers all Resilience4j aspects as Spring AOP beans with configurable order. Setting numeric aspect order values ensures the constitutionally required call chain, regardless of annotation order on method declarations.

**Aspect order configuration** (lower = outermost = highest priority):
```properties
resilience4j.retry.retry-aspect-order=1
resilience4j.circuitbreaker.circuit-breaker-aspect-order=2
resilience4j.ratelimiter.rate-limiter-aspect-order=3
resilience4j.timelimiter.time-limiter-aspect-order=4
resilience4j.bulkhead.bulkhead-aspect-order=5
```

**Service naming convention** (four independent instances per pattern):
- `car-api`, `client-api`, `sales-api`, `keycloak`

**Sales POST never retried**: Service methods annotated with `@CircuitBreaker` + `@RateLimiter` + `@TimeLimiter` + `@Bulkhead` but NOT `@Retry`. Only GET/safe operations carry `@Retry`.

**Semaphore bulkhead on Sales API**: `resilience4j.bulkhead.instances.sales-api.max-concurrent-calls` externalized. Type is `SEMAPHORE` (default in Resilience4j for `@Bulkhead`).

**All config externalized** to `application.properties`. No `@Bean` configuration hardcodes values.

**Alternatives considered**: Spring Cloud Circuit Breaker abstraction. Rejected — adds indirection without benefit; direct Resilience4j annotations with explicit ordering give full control over the five-layer chain specified in Article VI.

---

## R-03: Spring Security OAuth2 Resource Server — Local JWT Validation

**Decision**: Use `spring-boot-starter-oauth2-resource-server`. Configure JWKS URI via `spring.security.oauth2.resourceserver.jwt.jwk-set-uri`. No custom token validation logic.

**Rationale**: Spring Security's JWT resource server validates tokens locally using keys fetched from Keycloak's JWKS endpoint and cached in memory. This avoids Keycloak round-trips per request, satisfies Article IV ("token validation must use Spring Security OAuth2 Resource Server"), and matches the pattern used by Car API, Client API, and Sales API in the platform.

**Roles extraction**: Keycloak issues roles in either a top-level `roles` claim or nested under `realm_access.roles`. A `RolesClaimConverter` (same pattern as Car API's `SecurityConfig.RolesClaimConverter`) extracts them and maps to `ROLE_`-prefixed `GrantedAuthority` objects.

**Subject extraction**: `jwt.getSubject()` (the `sub` claim) yields the Keycloak user ID. Used for ownership enforcement in profile and purchase operations.

**Transparent token refresh**: Implemented as `TokenRefreshFilter extends OncePerRequestFilter`. When `SecurityContextHolder` is empty (no valid token in request) AND the HttpOnly refresh token cookie is present, the filter calls `AuthService.refreshToken()`, which exchanges the cookie value for a new access/refresh token pair via Keycloak, writes the new refresh token back to the cookie, and injects the new access token into `SecurityContextHolder` so the request proceeds normally.

**Alternatives considered**: Custom JWT validation with `nimbus-jose-jwt` directly. Rejected — Article VI explicitly mandates Spring Security OAuth2 Resource Server; custom implementations are constitutionally prohibited.

---

## R-04: WireMock Spring Boot Integration for Spring Boot 4.x

**Decision**: Use `org.wiremock.integrations:wiremock-spring-boot` version 3.6.0 (or latest compatible at implementation time). Declare four `@WireMockTest`-annotated fields, one per downstream client.

**Rationale**: `wiremock-spring-boot` provides the `@EnableWireMock` and `@InjectWireMock` annotations that integrate WireMock servers into the Spring `ApplicationContext`, making them available via `@Value` injection of the randomly-assigned port. This is the only mechanism that allows multiple isolated WireMock server instances per integration test run without manual server lifecycle management.

**One server per client pattern** (per Article IX mandate):
```java
@EnableWireMock({
    @ConfigureWireMock(name = "car-api-mock",    property = "feign.client.config.car-api.url"),
    @ConfigureWireMock(name = "client-api-mock", property = "feign.client.config.client-api.url"),
    @ConfigureWireMock(name = "sales-api-mock",  property = "feign.client.config.sales-api.url"),
    @ConfigureWireMock(name = "keycloak-mock",   property = "feign.client.config.keycloak.url")
})
```

**Keycloak WireMock**: Stubs the JWKS endpoint (`/realms/dealership/protocol/openid-connect/certs`), the token endpoint (`/realms/dealership/protocol/openid-connect/token`), and the admin user deletion endpoint (`/admin/realms/dealership/users/{id}`).

**Alternatives considered**: `MockServer` (org.mock-server). Rejected — `wiremock-spring-boot` is the explicit library named in Article IX and in the user's technical specification.

---

## R-05: springdoc-openapi Version for Spring Boot 4.x

**Decision**: Use `org.springdoc:springdoc-openapi-starter-webmvc-ui` version 3.0.2 (same version already in use by Car API, Client API, and Sales API in this platform).

**Rationale**: Version 3.0.2 is confirmed compatible with Spring Boot 4.0.x across all three platform APIs. Using the same version ensures consistent Swagger UI behavior and prevents classpath divergence. Version is not covered by Spring Cloud BOM, so it must be declared explicitly in `pom.xml`.

**Envelope schema registration**: `OpenApiConfig` registers `ApiResponse`, `ApiErrorResponse`, `ErrorBody`, and `ResponseMeta` as reusable components via `@Schema`. All endpoint `@ApiResponse` annotations reference these schemas by `$ref`, ensuring frontend developers see the full envelope structure in Swagger UI.

---

## R-06: PITest 1.19.1 Maven Plugin — Integration Test Exclusion and auto_threads

**Decision**: Declare `org.pitest:pitest-maven` 1.19.1 and `org.pitest:pitest-junit5-plugin` 1.2.3 explicitly (not in Spring Cloud BOM). Configure `<excludedTestClasses>` to exclude `integrated.**`. Enable `auto_threads` via `<features>+AUTO_THREADS</features>`. Set `<mutationThreshold>90</mutationThreshold>` and `<coverageThreshold>90</coverageThreshold>`.

**Rationale**: The `auto_threads` feature (`+AUTO_THREADS`) lets PITest select thread count based on available CPU cores, avoiding overcommit on CI while maximizing throughput locally. Excluding `integrated.**` prevents PITest from running WireMock/Testcontainers tests, which would require Docker and would dramatically inflate mutation analysis time.

**Excluded from mutation analysis** (no behavior worth mutating):
- `DealershiBffApplication` (main class)
- All `config.*` classes (Spring wiring, no business logic)
- All envelope record types: `ApiResponse`, `ApiErrorResponse`, `ResponseMeta`, `ErrorBody`, `ErrorDetail` (Article IX: "must not be run against generated code, configuration classes, or envelope record types")

**JVM arg required for Java 25**:
```xml
<jvmArgs>
    <jvmArg>--add-opens=java.base/java.lang=ALL-UNNAMED</jvmArg>
</jvmArgs>
```

**Alternatives considered**: PITest 1.17.0 (used by Car API). Rejected — Article II mandates 1.19.1.

---

## R-07: Transparent Token Refresh Architecture

**Decision**: Implement `TokenRefreshFilter extends OncePerRequestFilter` with order higher than Spring Security's `SecurityContextPersistenceFilter` but lower than `UsernamePasswordAuthenticationFilter`. Filter reads the refresh token cookie, calls `AuthService.refreshToken()` (which calls Keycloak via Feign), writes the new refresh token back as `HttpOnly; Secure; SameSite=Strict` cookie, sets the new access token into the security context, then calls `filterChain.doFilter()`.

**Rationale**: Placing the refresh in a `OncePerRequestFilter` before the JWT processing filter ensures that the valid access token is available for all downstream Spring Security checks, including method-level `@PreAuthorize`. The filter is a no-op when a valid access token is already present in the `Authorization` header, adding negligible overhead on happy-path requests.

**Failure handling**: If the refresh token is present but Keycloak rejects it (expired, revoked), the filter clears the stale cookie and lets the request fall through without a token — Spring Security then returns 401 in the standard envelope via `GlobalExceptionHandler`.

**Refresh token cookie attributes**:
- `HttpOnly: true`
- `Secure: true`
- `SameSite: Strict`
- `Path: /api/v1/auth` (scoped to auth endpoints; cannot be sent to inventory or other paths by mistake)

**Alternatives considered**: Intercepting 401 responses at the client side and calling a `/refresh` endpoint. Rejected — this exposes the refresh flow to JavaScript, violates Article IV ("refresh token must never be exposed to JavaScript"), and requires the frontend to implement retry logic.

---

## R-08: CompletableFuture Execution — Virtual Threads Executor

**Decision**: Configure an `AsyncConfig` bean that provides a `java.util.concurrent.Executor` backed by `Thread.ofVirtual().factory()` (Java 25 virtual threads). All `CompletableFuture.supplyAsync(...)` calls in `PurchaseService` use this executor explicitly.

**Rationale**: Spring Boot 4.0.x with `spring.threads.virtual.enabled=true` enables virtual threads for the Tomcat thread pool. To ensure that `CompletableFuture` tasks also use virtual threads (rather than the ForkJoinPool common pool), an explicit executor is provided. This eliminates blocking behavior when Feign calls park waiting for I/O, and aligns with Article V's performance constraints.

**Fail-fast implementation in purchase flow**:
```java
CompletableFuture<CarApiCarResponse>    carFuture    = supplyAsync(() -> carApiClient.getById(carId), executor);
CompletableFuture<ClientApiClientResponse> clientFuture = supplyAsync(() -> clientApiClient.getMe(token), executor);

CompletableFuture.allOf(carFuture, clientFuture)
    .exceptionally(ex -> { carFuture.cancel(true); clientFuture.cancel(true); throw wrap(ex); })
    .join();
```

---

## R-09: Registration Compensation Pattern

**Decision**: In `AuthService.register()`, execute Keycloak user creation first, then Client API profile creation. Wrap in a try/catch: on Client API failure, call Keycloak delete user endpoint. If deletion also fails, log the Keycloak subject ID at ERROR level with the request ID for manual reconciliation.

**Rationale**: There is no distributed transaction. The compensation is best-effort. The critical requirement is that the user sees a clean error and can retry. The Keycloak ROPC flow will reject a duplicate email/username on retry, so the cleanup is necessary. The log record provides an audit trail for support.

**Idempotency on retry**: If Keycloak deletion succeeded but Client API creation still fails on retry, the next registration attempt creates a new Keycloak user with a new subject ID — no orphan remains from the first attempt.

---

## R-10: Cache Key Design for Inventory

**Decision**: Use Spring Cache with Redis backend. Cache names:
- `"car-by-id"` — key: `#carId` (UUID string)
- `"car-listings"` — key: result of `InventoryFilterRequest.toCacheKey()`, a deterministic string built by sorting all non-null filter parameters alphabetically with page/size appended.

**Rationale**: Deterministic key generation (sort + concatenate) ensures that two requests with the same parameters but different query string ordering hit the same cache entry. Spring Cache's `@Cacheable` with a `SpEL` key expression handles the eviction by key on sale.

**Eviction**: `@CacheEvict(value = "car-by-id", key = "#carId")` in `PurchaseService.purchase()` after a successful sales API call. `car-listings` entries are NOT individually evicted on sale (impossible without tracking all keys); instead, the 5-minute TTL is the safety net for listing staleness, per Article VIII.

---

## Summary: All NEEDS CLARIFICATION Resolved

| Item | Resolution |
|------|-----------|
| OpenFeign version + config pattern | Spring Cloud BOM 2025.1.0; per-client `connect-timeout` + `read-timeout` in properties |
| Resilience4j chain order | Aspect order properties 1-5; Sales POST never retried; semaphore bulkhead on Sales |
| Spring Security + Keycloak JWT | OAuth2 Resource Server; JWKS URI; `RolesClaimConverter`; `sub` claim for ownership |
| WireMock Spring Boot 4.x compat | `org.wiremock.integrations:wiremock-spring-boot` 3.6.0; 4 servers via `@EnableWireMock` |
| springdoc-openapi version | 3.0.2 (same as platform APIs) |
| PITest + auto_threads + exclusions | Version 1.19.1 + 1.2.3; `+AUTO_THREADS`; thresholds=90; `integrated.**` excluded |
| Transparent token refresh | `TokenRefreshFilter` before JWT processing; Keycloak Feign call; scoped cookie path |
| CompletableFuture executor | Virtual threads executor; `allOf` with cancel-on-failure for purchase assembly |
| Registration compensation | Best-effort Keycloak delete; ERROR log with subject ID on compensation failure |
| Cache key for listings | `InventoryFilterRequest.toCacheKey()` deterministic sort; TTL=5 min; `car-by-id` evicted on sale |
