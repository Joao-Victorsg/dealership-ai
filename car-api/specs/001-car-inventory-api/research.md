# Research: Car Inventory API

**Feature**: 001-car-inventory-api  
**Date**: 2026-04-12  
**Status**: Complete

## R-001: Spring Boot 4.x + Java 25 Virtual Threads

**Decision**: Enable virtual threads via `spring.threads.virtual.enabled=true` for all request-handling threads. Use `StructuredTaskScope` for fan-out patterns (e.g., parallel cache invalidation + S3 deletion). Use `ScopedValue` instead of `ThreadLocal` where request-scoped context propagation is needed.

**Rationale**: Spring Boot 4.x has first-class support for virtual threads. All I/O-bound operations (database queries, Redis calls, S3 operations) benefit from virtual threads without platform thread pool sizing concerns. Virtual threads eliminate the need for reactive programming while achieving similar throughput for I/O-bound workloads.

**Alternatives considered**:
- WebFlux (reactive): Rejected — adds complexity, requires reactive drivers throughout, and the team's stack is servlet-based. Virtual threads provide equivalent I/O concurrency with simpler synchronous code.
- Platform threads with tuned pool: Rejected — requires manual pool sizing and limits scalability under concurrent I/O.

## R-002: Spring Data Redis Caching Strategy

**Decision**: Use Spring Cache abstraction (`@Cacheable`, `@CacheEvict`) backed by `RedisCacheManager`. Define two cache regions:
1. `car-by-id` — keyed by car UUID, TTL 24h
2. `car-listings` — keyed by composite hash of filter+sort+page params, TTL 24h

On car register or update: evict the specific `car-by-id` entry and evict ALL entries from `car-listings` (full region invalidation).

**Rationale**: Individual car lookups are easily keyed by ID. Listing/filter caches use a deterministic composite key derived from all query parameters. Full listing cache eviction on writes ensures no stale filtered results persist. The 24h TTL provides a safety net for any missed invalidation. Redis `FLUSHDB` is not used — instead, Spring's `@CacheEvict(allEntries = true)` on the `car-listings` region uses Redis `SCAN` + `DEL` with a key prefix pattern.

**Alternatives considered**:
- Fine-grained listing invalidation (evict only affected filter combos): Rejected — combinatorial explosion of possible filter combinations makes targeted invalidation impractical and error-prone.
- Cache-aside without Spring abstraction: Rejected — Spring Cache abstraction reduces boilerplate, is well-tested, and integrates cleanly with Spring Boot auto-configuration.
- Caffeine local cache: Rejected — does not work across multiple service instances. Redis is required for distributed caching.

## R-003: AWS SDK v2 S3 Presigned URLs

**Decision**: Use `software.amazon.awssdk:s3` and `software.amazon.awssdk:s3-presigner` to generate presigned PUT URLs. The presigned URL has a short TTL (15 minutes). The S3 client is configured as a Spring `@Bean` using `S3Client.builder()` with region and credentials from environment/IAM role. The presigner uses `S3Presigner.builder()`.

**Rationale**: AWS SDK v2 is the current generation SDK with async support and modern API design. Presigned URLs allow direct client-to-S3 uploads without proxying through the API, reducing bandwidth and latency. The short TTL limits the window for misuse.

**Upload flow**:
1. Authenticated client calls `POST /api/v1/cars/{id}/image/presigned-url` with content type
2. Car API generates a unique S3 object key (`cars/{carId}/{uuid}.{ext}`)
3. Car API returns presigned PUT URL + generated object key
4. Client uploads directly to S3 using the presigned URL
5. Client calls `PATCH /api/v1/cars/{id}` with `imageKey` = the returned object key
6. Car API persists the key and deletes the previous S3 object (if any)

**Alternatives considered**:
- Multipart upload through API: Rejected — increases API server bandwidth, memory pressure, and latency. Direct S3 upload is the standard pattern for scalable image handling.
- AWS SDK v1: Rejected — end-of-life. SDK v2 is the supported path.

## R-004: Spring Security OAuth2 Resource Server

**Decision**: Use `spring-boot-starter-security` + `spring-boot-starter-oauth2-resource-server` for JWT validation. Configure the `SecurityFilterChain` to:
- Permit all on `GET /api/v1/cars/**` (public read)
- Permit all on actuator health endpoints
- Permit all on OpenAPI/Swagger UI paths
- Require authentication with `ROLE_STAFF` or `ROLE_ADMIN` for `POST` and `PATCH` on `/api/v1/cars/**`
- Deny all other requests

JWT issuer URI is configured via `spring.security.oauth2.resourceserver.jwt.issuer-uri`. Roles are extracted from the JWT claims (claim name configurable, default: `roles` or `realm_access.roles` for Keycloak-style tokens).

**Rationale**: Spring Security's OAuth2 Resource Server provides battle-tested JWT validation with minimal configuration. Token validation is stateless — no session or database lookup required. The external IdP handles user management, which is out of scope for this service.

**Alternatives considered**:
- Custom JWT filter: Rejected — reinvents what Spring Security provides out of the box, with higher risk of security bugs.
- Session-based authentication: Rejected — the service is stateless and consumed by a BFF, not directly by a browser.

## R-005: Flyway Migration Strategy

**Decision**: Use Flyway with versioned migrations in `src/main/resources/db/migration/`. The first migration (`V1__create_car_table.sql`) creates the `car` table with all columns and indexes. Subsequent schema changes follow `V{N}__{description}.sql` naming.

Database indexes created in V1:
- `idx_car_status` on `status`
- `idx_car_category` on `category`
- `idx_car_manufacturer` on `manufacturer`
- `idx_car_propulsion_type` on `propulsion_type`
- `idx_car_year` on `manufacturing_year`
- `idx_car_value` on `listed_value`
- `uq_car_vin` unique constraint on `vin`
- Composite index `idx_car_status_category` on `(status, category)` for common combined filters

**Rationale**: Flyway is already in the project dependencies. Versioned migrations provide a clear, auditable history of schema changes. Creating all indexes upfront avoids performance issues as the dataset grows.

**Alternatives considered**:
- JPA auto-DDL (`spring.jpa.hibernate.ddl-auto=update`): Rejected — unsafe for production, does not produce repeatable migrations, cannot be audited.
- Liquibase: Rejected — Flyway is already chosen and configured. Both are capable; switching adds no value.

## R-006: Testcontainers Strategy (Redis + LocalStack S3)

**Decision**: Extend the existing `TestcontainersConfiguration.java` to include:
1. `GenericContainer` for Redis (using `redis:7-alpine` image) with `@ServiceConnection`
2. `LocalStackContainer` for S3 integration tests (using `localstack/localstack:latest`)

The PostgreSQL container is already configured. All containers use `@ServiceConnection` where supported by Spring Boot, or manual property injection for LocalStack.

**Pattern**: Follow the existing project convention (see `TestcontainersConfiguration.java` and the referenced LocalStack pattern from the dealership repo).

**Rationale**: Testcontainers provides real infrastructure for integration tests without mocks. Redis and S3 behavior cannot be reliably simulated with mocks for cache invalidation and presigned URL workflows.

**Alternatives considered**:
- Embedded Redis (e.g., `it.ozimov:embedded-redis`): Rejected — abandoned library, compatibility issues with newer Redis features.
- Mocking Redis/S3 in integration tests: Rejected — defeats the purpose of integration testing. Real containers ensure behavior matches production.

## R-007: Pagination Strategy

**Decision**: Use Spring Data's `Pageable` with `Page<Car>` responses. Default page size: 20, maximum page size: 100. Pagination parameters are `page` (0-based) and `size`. The response includes `totalElements`, `totalPages`, `number` (current page), and `size`.

**Rationale**: Spring Data JPA has built-in pagination support via `PagingAndSortingRepository`. This integrates directly with query methods and avoids custom pagination logic.

**Alternatives considered**:
- Cursor-based pagination: Rejected — overkill for a catalog of ≤10,000 items. Offset-based pagination is simpler and sufficient at this scale. Can be revisited if scale requirements change.
- No pagination: Rejected — spec requires it (FR-020), and unbounded queries are a performance risk.

## R-008: Error Response Format

**Decision**: All error responses follow a consistent JSON structure:
```json
{
  "timestamp": "2026-04-12T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "fieldErrors": [
    {
      "field": "vin",
      "message": "VIN must be exactly 17 alphanumeric characters"
    }
  ]
}
```

A `@RestControllerAdvice` (`GlobalExceptionHandler`) handles all exceptions and maps them to this format. Validation errors (from Bean Validation) produce field-level error details. Business rule violations produce descriptive messages.

**Rationale**: Consistent error format satisfies UXR-002. Field-level errors allow consuming BFF to present actionable feedback. The format aligns with Spring Boot's default error structure but adds the `fieldErrors` array.

**Alternatives considered**:
- RFC 7807 Problem Details: Considered — Spring Boot 4.x supports it natively. However, the `fieldErrors` extension is needed for validation, and the simpler custom format is more explicit for this project's needs. Could migrate to RFC 7807 in a future iteration.

## R-009: VIN Normalization

**Decision**: VINs are normalized to uppercase on input (both registration and filtering). The database stores VINs in uppercase. The unique constraint on `vin` ensures case-insensitive uniqueness via this normalization.

**Rationale**: Industry convention is uppercase VINs. Normalizing at the API boundary eliminates case-sensitivity issues without requiring a case-insensitive database index.

**Alternatives considered**:
- Case-insensitive unique index (`UPPER(vin)`): Rejected — adds database complexity when normalization at the application layer is simpler and more explicit.
- Reject lowercase VINs: Rejected — spec explicitly states the system accepts and normalizes them (Edge Cases section).

