# Research: Client API — Customer Profile Management

**Feature**: `001-client-api-profile`  
**Phase**: 0 — Design unknowns resolved before Phase 1  
**Date**: 2026-04-17

---

## Decision 1 — CPF Encryption at Persistence Layer

**Decision**: Use a JPA `AttributeConverter<String, String>` that performs AES-256-GCM
encryption/decryption transparently. The converter is annotated with `@Convert` on the
`cpf` field of the JPA entity. The service layer deals only with plaintext CPF strings;
encryption is invisible above the persistence layer.

**Rationale**: The constitution requires CPF to be encrypted at rest and never logged in
plaintext. An `AttributeConverter` is the standard JPA mechanism for field-level
transformation. It encapsulates the concern entirely within the persistence layer, keeping
service code clean. AES-256-GCM is authenticated encryption — it provides both
confidentiality and integrity, preventing silent data corruption.

**Alternatives considered**:
- *Column-level encryption in PostgreSQL (pgcrypto)*: Requires database-specific SQL
  functions, makes migrations complex, and couples the application to a specific DB feature.
  Rejected.
- *Application-level encryption in service layer*: Would require the service to handle
  plaintext → ciphertext conversion, leaking the concern into business logic. Rejected.

**Key implementation notes**:
- Encryption key sourced from environment variable / Secrets Manager; never hardcoded.
- The `@Column` storing the encrypted value must be sized for base64-encoded ciphertext
  (CPF is 11 digits; with AES-GCM overhead and base64 encoding, 100 chars is sufficient).
- The `AttributeConverter` must also ensure the CPF is never included in entity
  `toString()` output — Lombok's `@ToString(exclude = "cpf")` enforces this.

---

## Decision 2 — JWT Role Extraction from `realm_access.roles`

**Decision**: Implement a `Converter<Jwt, AbstractAuthenticationToken>` (a
`JwtAuthenticationConverter`) that reads the `realm_access.roles` claim array from the
JWT and maps each entry to a `GrantedAuthority` with the `ROLE_` prefix. This converter
is registered in the Spring Security OAuth2 Resource Server configuration.

**Rationale**: Spring Boot's default JWT converter maps roles from the `scope` or
`scp` claim. Keycloak places realm roles under `realm_access.roles`, which requires
a custom converter. This is the standard, documented Spring Security extension point
for customising role extraction — no custom token validation logic is needed beyond
this mapping.

**Alternatives considered**:
- *Custom `AuthenticationProvider`*: More invasive and bypasses the standard OAuth2
  Resource Server infrastructure. Rejected.
- *Parsing roles inside each controller method*: Violates separation of concerns; roles
  would not be available via `@PreAuthorize`. Rejected.

**Key implementation notes**:
- `JwtGrantedAuthoritiesConverter` for scope (disabled or kept empty), combined with a
  custom converter for realm roles, registered via `JwtAuthenticationConverter`.
- `@EnableMethodSecurity` enables `@PreAuthorize` at the method level.
- `SecurityContextHolder` provides access to the authenticated JWT for subject ID extraction.

---

## Decision 3 — Resilience4j Circuit Breaker for ViaCEP

**Decision**: Wrap the ViaCEP HTTP call in a `@CircuitBreaker(name = "viacep", fallbackMethod = "...")` 
from Resilience4j Spring Boot 3 starter (already in `pom.xml`). The fallback returns an empty
`Optional<ViaCepResponse>`. The calling service interprets an absent result as a lookup failure and
proceeds with partial address data and `isAddressSearched = false`.

**Rationale**: The constitution mandates that ViaCEP failure never propagates as a 500 to the
caller. Resilience4j's `@CircuitBreaker` provides open/half-open/closed state management with
configurable thresholds, without requiring manual try/catch across callers. The fallback method
pattern cleanly decouples the failure handling from business logic.

**Alternatives considered**:
- *Manual try/catch around HTTP call*: Works but doesn't provide circuit state management;
  a permanently-down ViaCEP would still hammer the downstream service on every request.
  Rejected.
- *Resilience4j `@Retry`*: Retry is unsuitable here — the constitution requires the registration
  to succeed immediately without waiting for retries. The external retry service handles
  later resolution. Rejected as primary mechanism.

**Key implementation notes**:
- Circuit breaker configuration in `application.properties`:
  `resilience4j.circuitbreaker.instances.viacep.sliding-window-size=10`
  `resilience4j.circuitbreaker.instances.viacep.failure-rate-threshold=50`
  `resilience4j.circuitbreaker.instances.viacep.wait-duration-in-open-state=30s`
- Timeout configured separately via `resilience4j.timelimiter` to prevent slow ViaCEP
  responses from blocking the request thread.
- ViaCEP base URL and timeout configurable via `application.properties`.

---

## Decision 4 — Redis Cache Strategy

**Decision**: Use Spring's `@Cacheable` / `@CacheEvict` annotations backed by
`spring-boot-starter-data-redis`. Cache key: `client::{uuid}`. TTL: 24 hours, configured
in `RedisCacheConfiguration`. Cache eviction on every write (update, address update, CPF
correction, deletion/anonymization) using `@CacheEvict(key = "#id")` before the
operation returns.

**Rationale**: The constitution requires that GET operations hit cache first with 24h TTL,
and that every write invalidates the cache before returning. Spring's cache abstraction
provides a clean declarative model. Cache-aside with evict-on-write (rather than update-on-write)
is the safer pattern — it eliminates the risk of stale data in the cache after a failed write.

**Alternatives considered**:
- *Manual `RedisTemplate` calls in the service layer*: More control but much more boilerplate.
  The annotation approach is sufficient given the straightforward cache key structure.
  Rejected.
- *Cache-through (write to cache on write)*: More complex; a failed cache write could leave
  stale data. Evict-on-write ensures the next read fetches fresh data from DB. Rejected.

**Key implementation notes**:
- Anonymized profiles must NOT be cached. After anonymization, `@CacheEvict` removes the
  entry, and the anonymized profile is never passed to a cacheable method.
- `RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(24))`.
- Serialization: JSON via `Jackson2JsonRedisSerializer` (avoids Java serialization issues
  across deployments).

---

## Decision 5 — Flyway Migration Strategy

**Decision**: Versioned migrations under `src/main/resources/db/migration/`. Naming:
`V{version}__{description}.sql`. Initial migration creates the `clients` table with all
columns, unique indexes on `keycloak_id` and `cpf`, and the `deleted_at` column for soft
deletes.

**Rationale**: Flyway is already included in `pom.xml`. Versioned migrations provide
deterministic, repeatable schema evolution. The description convention is Flyway's standard.

**Alternatives considered**:
- *Hibernate `ddl-auto=create`*: Unsuitable for production; loses schema control and makes
  migrations non-repeatable. Rejected.

**Key implementation notes**:
- `spring.flyway.baseline-on-migrate=true` for environments where the database may pre-exist.
- The `cpf` column stores the encrypted ciphertext — column type `VARCHAR(100)`.
- A separate `deleted_at TIMESTAMP` column supports the soft-delete pattern.
- Unique constraint on `cpf` column still works with encrypted values because the same
  plaintext CPF always produces the same ciphertext (deterministic encryption mode, or a
  separate hash column for uniqueness check — see key note below).

**⚠️ Encryption uniqueness note**: AES-GCM uses a random IV per encryption, meaning the
same plaintext CPF will produce different ciphertext on each call. This breaks a `UNIQUE`
DB constraint. Resolution:
- **Option A** (recommended): Store a separate `cpf_hash` column (HMAC-SHA256 of CPF +
  server secret), apply `UNIQUE` constraint on `cpf_hash`. The `cpf` column holds the
  AES-GCM ciphertext. Lookup by CPF uses the hash.
- **Option B**: Use deterministic encryption (AES-SIV or AES-ECB) — weaker security
  properties; not recommended.

**Decision**: Use Option A. The `cpf_hash` column is indexed and unique; the `cpf` column
stores the GCM ciphertext. The `AttributeConverter` stores ciphertext in `cpf`;
a separate `@PrePersist`/`@PreUpdate` hook (or a second converter) stores the HMAC in
`cpf_hash`. Uniqueness and lookup are done via `cpf_hash`.

---

## Decision 6 — Soft Delete / Anonymization Pattern

**Decision**: Implement anonymization as an in-place update. A `delete` operation on
the `Client` entity:
1. Replaces `firstName`, `lastName`, `phoneNumber` with a fixed anonymized placeholder
   (e.g., `"ANONYMIZED"`).
2. Replaces `cpf` and `cpf_hash` with a UUID-derived value that is unique but
   non-identifiable.
3. Replaces all address fields with empty strings.
4. Replaces `keycloakId` with a generated random UUID string.
5. Sets `deletedAt = now()`.

No record is physically deleted. The `@Where` annotation (or `@Filter`) is NOT used —
anonymized records are handled by service-layer checks (`deletedAt != null` → 422).

**Rationale**: Physical records must be retained for referential integrity with historical
sales records. The anonymization removes all PII while keeping the primary key. Checking
`deletedAt` in the service layer is more explicit than JPA-level soft-delete filters,
which can be accidentally bypassed.

**Alternatives considered**:
- *Hibernate `@SQLRestriction`*: Would filter out deleted records at query level; could
  accidentally hide records from admin lookups that may need them. More complex to override.
  Rejected.

---

## Decision 7 — Request Logging Without PII

**Decision**: Implement a `HandlerInterceptor` or Servlet `Filter` that logs each inbound
request's method, path, and response status + latency after the response is committed.
The filter MUST NOT log the request body. Response bodies are also not logged. Address
resolution outcomes (ViaCEP success/failure + resulting `isAddressSearched`) are logged
as structured MDC entries inside the `ClientService`.

**Rationale**: The constitution forbids logging request bodies (PII risk). A filter/interceptor
that captures method + path + status + latency after response commit is the standard approach.
Structured logging via SLF4J MDC allows filtering/querying address retry outcomes in log
aggregation tools.

**Alternatives considered**:
- *Spring Boot's `CommonsRequestLoggingFilter`*: Logs request bodies by default — must be
  disabled or not used. Too risky given the PII constraints. Rejected.

---

## Decision 8 — New Relic Observability

**Decision**: The New Relic Java Agent is attached at JVM startup via `-javaagent` flag
in the deployment configuration. Configuration via `newrelic.yml`. Zero New Relic SDK
imports in application code. Spring Boot Actuator `/actuator/health` and
`/actuator/health/readiness` are exposed and used by infrastructure monitoring.

**Rationale**: The constitution explicitly forbids New Relic SDK calls in application code.
The Java agent handles all instrumentation transparently.

**Key implementation notes**:
- `management.endpoints.web.exposure.include=health` (at minimum).
- `management.endpoint.health.probes.enabled=true` for readiness/liveness probes.
- New Relic agent version and `newrelic.yml` managed outside the application codebase
  (in deployment config / Helm chart / Dockerfile). Not committed to source.
