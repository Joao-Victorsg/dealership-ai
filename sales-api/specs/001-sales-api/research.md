# Research: Sales API

**Phase**: 0 — Pre-Design Research  
**Branch**: `001-sales-api`  
**Date**: 2026-04-19

---

## 1. Resilience4j Stacking — Retry → Circuit Breaker → Time Limiter

### Problem
The SNS publish must be protected by three Resilience4j decorators in the
order Retry → Circuit Breaker → Time Limiter. Resilience4j annotations use
Spring AOP aspect ordering, which can produce a different wrapping order than
expected without explicit configuration.

### Research Finding
Resilience4j Spring Boot default aspect order (lower = outermost):
- TimeLimiter: 2
- Retry: 3
- CircuitBreaker: 4

By default this produces `TimeLimiter(Retry(CircuitBreaker(call)))`.
To get the constitution's required `Retry(CircuitBreaker(TimeLimiter(call)))`,
the aspect ordering must be overridden.

**Additionally**: `@TimeLimiter` with annotations requires the method to return
`CompletableFuture<T>`, which conflicts with the transactional service pattern
(SNS publish must block before the transaction commits).

### Decision
**Use `@Retry` and `@CircuitBreaker` annotations (synchronous) and rely on
AWS SDK v2 `ClientOverrideConfiguration.apiCallTimeout(2s)` for time bounding.**

Rationale:
- Avoids `CompletableFuture` complexity in transactional context
- `apiCallTimeout` on the SnsClient achieves identical behavior to
  `TimeLimiter` (caps the blocking duration at 2 seconds)
- Retry and CircuitBreaker annotations are synchronous and composable
- Ordering is enforced via properties: `retry-aspect-order < circuit-breaker-aspect-order`

Configuration:
```properties
resilience4j.retry.retry-aspect-order=2
resilience4j.circuitbreaker.circuit-breaker-aspect-order=3
```

This produces `Retry(CircuitBreaker(actual_call))` where `actual_call` is
already time-bounded by the SDK.

### Alternatives Considered
- **Programmatic `Decorators.ofSupplier()`**: More explicit but adds
  `ScheduledExecutorService` complexity and loses Spring Boot autoconfiguration.
- **Full annotation stacking with `CompletableFuture`**: Requires threading model
  changes that complicate the `@Transactional` atomicity contract.

---

## 2. Transactional + SNS Atomicity (Both or Neither)

### Problem
The sale must not be committed to the database if the SNS publish fails.
SNS is not an XA resource, so true 2-phase commit is not possible.

### Research Finding
Spring `@Transactional` rolls back on unchecked (`RuntimeException`) exceptions.
The JPA `save()` writes to the database but the commit only happens when the
transaction boundary exits normally. If any `RuntimeException` is thrown inside
the transaction, the transaction manager rolls back before commit.

### Decision
**Persist first (within transaction), then publish SNS (within same transaction
boundary). Any `RuntimeException` from SNS publish rolls back the transaction.**

Flow:
```
@Transactional
registerSale() {
    sale = saleRepository.save(buildSale(...))
    // JDBC INSERT is staged; commit not yet flushed
    snsPublisher.publish(buildEvent(sale))   // throws SnsPublishException if fails
    return SaleResponse.from(sale)           // commit occurs here (method exit)
}
```

If `snsPublisher.publish()` throws `SnsPublishException` (a `RuntimeException`),
Spring rolls back the transaction. The JDBC INSERT is never committed.

The service MUST NOT call `flush()` before the SNS publish, as early flushing
would make the insert visible to concurrent queries before SNS succeeds.

Caveat: SNS publishes successfully but the commit then fails (extremely unlikely).
This is an accepted risk (at-most-once delivery), documented as a known limitation.

### Alternatives Considered
- **Transactional Outbox pattern**: Write SNS payload to an `outbox` table within
  the same transaction; a separate poller publishes to SNS. True at-least-once
  delivery but adds significant complexity, a second table, and a polling component.
  Rejected as over-engineered for the current scale.
- **Spring `@TransactionalEventListener`**: Publishes after commit. Does not satisfy
  the "both or neither" requirement — a commit followed by a publish failure would
  leave a persisted sale without an event.

---

## 3. WireMock JWKS + Nimbus JOSE+JWT for Integration Tests

### Problem
Spring Security OAuth2 Resource Server fetches the JWKS at startup from
`spring.security.oauth2.resourceserver.jwt.jwks-uri`. Integration tests need
a real RSA-signed JWT that Spring Security will accept, served from a
controllable endpoint.

### Research Finding
- **Nimbus JOSE+JWT 9.48** provides `RSAKey.generate(2048)` and `RSASSASigner`
  for generating real RSA keys and signing JWTs at test time.
- **WireMock Testcontainer** (`wiremock/wiremock:3.13.0`) serves the JWKS HTTP
  endpoint that Spring Security fetches on startup.
- The `EnvironmentInitializer` injects the WireMock container's base URL into
  the Spring environment before the `SecurityFilterChain` is initialized.
- Both containers must be `private static final` and started once per JVM.

### Decision
**Use `WireMockContainer` (wiremock/wiremock:3.13.0) serving the JWKS from a
static stub. Generate an RSA-2048 keypair once per test class. Sign all test
JWTs with the private key. Inject `jwks-uri` via `EnvironmentInitializer`.**

Key pair generation (once per JVM, static field in `BaseIT`):
```java
private static final RSAKey TEST_RSA_KEY = new RSAKeyGenerator(2048)
    .keyID("test-key-id")
    .generate();
```

JWKS response:
```java
var jwkSet = new JWKSet(TEST_RSA_KEY.toPublicJWK());
// WireMock stub: GET /realms/dealership/protocol/openid-connect/certs
//   → 200 application/json body = jwkSet.toString()
```

JWT generation helper:
```java
public static String buildToken(UUID subject, String... roles) {
    var signer = new RSASSASigner(TEST_RSA_KEY);
    var claims = new JWTClaimsSet.Builder()
        .subject(subject.toString())
        .audience("dealership")
        .issuer(wireMockContainer.getBaseUrl() + "/realms/dealership")
        .expirationTime(Date.from(Instant.now().plusSeconds(60)))
        .claim("realm_access", Map.of("roles", List.of(roles)))
        .build();
    var jwt = new SignedJWT(
        new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test-key-id").build(),
        claims);
    jwt.sign(signer);
    return jwt.serialize();
}
```

### Alternatives Considered
- **`@WithMockUser` / mocked `SecurityContext`**: Explicitly prohibited by
  constitution (Article XI). Does not exercise the real JWT validation path.
- **Embedded WireMock extension (`WireMockExtension`)**: Not a Docker container;
  runs in the same JVM. Spring's remote JWKS fetch requires an HTTP server
  addressable from the Spring context — `WireMockExtension` on a dynamic port
  works, but a container is more faithful to production topology and is what
  the constitution specifies.

---

## 4. LocalStack SNS + SQS for Integration Tests

### Problem
The SNS publish behavior must be tested end-to-end: the message must actually
be delivered to the SNS topic and assertable — not just that no exception was
thrown.

### Research Finding
- **LocalStack 4.4** supports both SNS and SQS.
- To assert SNS delivery, subscribe an SQS queue to the SNS topic.
  After sale registration, receive from the SQS queue and verify the payload.
- The AWS SDK v2 `SnsClient` and `SqsClient` are pointed at LocalStack via
  endpoint override in the `EnvironmentInitializer`.
- Topic and queue must be created once in a `@BeforeAll` (or static initializer).

### Decision
**Create SNS topic + SQS queue + SNS-to-SQS subscription in `@BeforeAll`.
After sale registration, poll SQS using `receiveMessage()` to assert delivery.**

Container config:
```java
private static final LocalStackContainer localStack =
    new LocalStackContainer(DockerImageName.parse("localstack/localstack:4.4"))
        .withServices(Service.SNS, Service.SQS);
```

`EnvironmentInitializer` injects:
```properties
app.sns.endpoint-override=${localstack.endpoint}
app.sns.region=us-east-1
app.sns.topic-arn=arn:aws:sns:us-east-1:000000000000:sale-events
```

SNS failure test: stub LocalStack topic via `overrideConfiguration` to
return `SnsException` consistently, or use Resilience4j test helpers to
force the circuit breaker open. Verify via `@Transactional` inspection
that no sale row exists.

### Alternatives Considered
- **Mock `SnsClient` in IT**: Avoids LocalStack but does not exercise real
  serialization, SDK retry behavior, or actual topic delivery. Prohibited
  by constitution which requires the message to be "actually delivered".
- **SNS FIFO topic**: Adds ordering guarantees not required here. Standard
  topic is sufficient for at-least-once downstream delivery.

---

## 5. Spring Cache Redis — Sale by ID

### Problem
Individual sale lookups (`GET /sales/{id}`) must be served from Redis cache
first (24h TTL). Lists must NOT be cached.

### Research Finding
From `car-api` `RedisConfig`: `RedisCacheManager` with `GenericJacksonJsonRedisSerializer`
configured per cache name with individual TTLs. `@Cacheable` annotation on
service method.

### Decision
**Configure `RedisCacheManager` with a `"sales"` cache (24h TTL, JSON serialization).
Annotate `getById()` service method with `@Cacheable(cacheNames = "sales", key = "#id")`.
No `@CacheEvict` needed — sales are immutable.**

```java
// RedisConfig
.withCacheConfiguration("sales",
    defaultConfig.entryTtl(Duration.ofHours(24)))
```

```java
// SaleService
@Cacheable(cacheNames = "sales", key = "#id")
@Transactional(readOnly = true)
public SaleResponse getById(UUID id, UUID requestingClientId, boolean isStaff) { ... }
```

List endpoints (`GET /sales` and `GET /sales/staff`) are NOT annotated
with `@Cacheable`. The combination of pagination, date filters, and client/car
filters makes key management impractical and hit rates low.

### Alternatives Considered
- **Cache list results**: Impractical due to filter permutations. Rejected.
- **`@CachePut` on registration**: Warms the cache eagerly. Adds complexity
  for negligible benefit on a write-once endpoint. Rejected.

---

## 6. AWS SDK v2 SnsClient Configuration

### Decision
Model `SnsConfig` after `car-api`'s `S3Config` pattern. Use
`DefaultCredentialsProvider` for production (IAM role in ECS). Support
`endpointOverride` from `SnsProperties` for local/test environments.
Apply `ClientOverrideConfiguration` with `apiCallTimeout(2s)` and
`apiCallAttemptTimeout(1s)` as the time-limiter equivalent.

```java
@Bean
SnsClient snsClient(SnsProperties properties) {
    var builder = SnsClient.builder()
        .credentialsProvider(DefaultCredentialsProvider.create())
        .region(Region.of(properties.region()))
        .overrideConfiguration(ClientOverrideConfiguration.builder()
            .apiCallTimeout(Duration.ofSeconds(2))
            .apiCallAttemptTimeout(Duration.ofSeconds(1))
            .build());
    if (StringUtils.hasText(properties.endpointOverride())) {
        builder.endpointOverride(URI.create(properties.endpointOverride()));
    }
    return builder.build();
}
```

AWS SDK version: `2.25.60` (aligned with `car-api`).

---

## 7. Missing pom.xml Dependencies

The following dependencies must be added to the current `pom.xml`:

| Dependency | GroupId : ArtifactId | Version | Scope |
|---|---|---|---|
| OAuth2 Resource Server | `org.springframework.boot:spring-boot-starter-oauth2-resource-server` | Boot BOM | compile |
| Spring Data Redis | `org.springframework.boot:spring-boot-starter-data-redis` | Boot BOM | compile |
| Resilience4j Spring Boot | `io.github.resilience4j:resilience4j-spring-boot3` | 2.3.0 | compile |
| AWS SDK v2 BOM | `software.amazon.awssdk:bom` | 2.25.60 | import (dependencyManagement) |
| AWS SDK SNS | `software.amazon.awssdk:sns` | via BOM | compile |
| AWS SDK SQS (test assertions) | `software.amazon.awssdk:sqs` | via BOM | test |
| Instancio JUnit 5 | `org.instancio:instancio-junit` | 5.3.0 | test |
| Rest Assured | `io.rest-assured:rest-assured` | 6.0.0 | test |
| WireMock Testcontainer | `org.wiremock.integrations:wiremock-testcontainers-module` | 1.0-alpha-14 | test |
| LocalStack Testcontainers | `org.testcontainers:localstack` | TC BOM | test |
| Nimbus JOSE+JWT | `com.nimbusds:nimbus-jose-jwt` | 9.48 | test |
| JaCoCo Maven Plugin | `org.jacoco:jacoco-maven-plugin` | 0.8.13 | plugin |
| PITest Maven Plugin | `org.pitest:pitest-maven` | 1.17.0 | plugin |
| PITest JUnit 5 Plugin | `org.pitest:pitest-junit5-plugin` | 1.2.1 | plugin dep |
| Maven Failsafe Plugin | `org.apache.maven.plugins:maven-failsafe-plugin` | inherited | plugin |
| Maven Surefire Plugin | `org.apache.maven.plugins:maven-surefire-plugin` | inherited | plugin |
| AOP Starter (for Resilience4j) | `org.springframework.boot:spring-boot-starter-aop` | Boot BOM | compile |

---

## Summary of All Decisions

| # | Topic | Decision |
|---|-------|----------|
| 1 | Resilience4j stacking | `@Retry` + `@CircuitBreaker` annotations; AWS SDK `apiCallTimeout(2s)` as time limiter |
| 2 | Transactional atomicity | `@Transactional` rollback on `SnsPublishException`; persist-then-publish order |
| 3 | WireMock JWKS + JWT | `WireMockContainer` (wiremock/wiremock:3.13.0); Nimbus RSA-2048 key; static containers |
| 4 | LocalStack SNS | `localstack/localstack:4.4`; SNS + SQS; SQS subscription for message assertion |
| 5 | Redis caching | `@Cacheable(cacheNames = "sales")` on `getById()`; 24h TTL; no list caching |
| 6 | AWS SDK SnsClient | `DefaultCredentialsProvider`; `endpointOverride` from `SnsProperties`; `apiCallTimeout(2s)` |
| 7 | Missing dependencies | 14 additional dependencies + 3 build plugins required |
