# Quickstart: Dealership BFF

**Repository**: `dealership-bff`
**Package**: `br.com.dealership.dealershibff`
**Parent repo**: `dealership-ai/` — contains `car-api/`, `client-api/`, `sales-api/`

---

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| JDK | 25 | Use [SDKMAN](https://sdkman.io/) or [Adoptium](https://adoptium.net/) |
| Maven | 3.9+ | Or use `./mvnw` wrapper |
| Docker | 26+ | For Redis (local dev) and integration tests |
| Docker Compose | v2 | Required for `compose.yaml` |

---

## Local Development Setup

### 1. Start Required Infrastructure

```bash
# From dealership-bff/
docker compose up -d
```

This starts:
- Redis (port 6379)
- (Downstream services must be started separately — see below)

### 2. Start Downstream Services

The BFF calls three platform APIs and Keycloak. For local development, start them from their respective folders:

```bash
# From dealership-ai/
(cd car-api    && ./mvnw spring-boot:run) &
(cd client-api && ./mvnw spring-boot:run) &
(cd sales-api  && ./mvnw spring-boot:run) &
```

Or use the docker-compose file at the parent `dealership-ai/` directory if one exists.

### 3. Configure Environment Variables

Copy the example properties and adjust for your local Keycloak:

```bash
cp src/main/resources/application.properties src/main/resources/application-local.properties
```

Then export or set in your IDE:

| Variable | Example | Description |
|----------|---------|-------------|
| `KEYCLOAK_BASE_URL` | `http://localhost:8080` | Keycloak server URL |
| `KEYCLOAK_REALM` | `dealership` | Keycloak realm name |
| `KEYCLOAK_CLIENT_ID` | `dealership-bff` | Keycloak confidential client ID |
| `KEYCLOAK_CLIENT_SECRET` | `<secret>` | Client secret from Keycloak admin |
| `CAR_API_BASE_URL` | `http://localhost:8081` | Car API base URL |
| `CLIENT_API_BASE_URL` | `http://localhost:8082` | Client API base URL |
| `SALES_API_BASE_URL` | `http://localhost:8083` | Sales API base URL |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |

### 4. Run the Application

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The BFF starts on **port 8084** by default.
Swagger UI: [http://localhost:8084/swagger-ui.html](http://localhost:8084/swagger-ui.html)

---

## Build

```bash
# Compile only
./mvnw compile

# Package (skip tests)
./mvnw package -DskipTests

# Full build (unit tests + JaCoCo)
./mvnw verify

# Full build + integration tests (requires Docker)
./mvnw verify -P integration-test
```

---

## Testing

### Unit Tests

Unit tests are in `src/test/java/br/com/dealership/dealershibff/`. They use JUnit 5, Mockito, and Instancio. No Docker or network required.

```bash
./mvnw test
```

### Integration Tests

Integration tests are in the `integrated.*` package (under `src/test/java/integrated/`). They use WireMock (4 independent servers per test class) and do NOT use Testcontainers for downstream services — WireMock stubs all four downstream APIs. Redis is started via Testcontainers.

```bash
./mvnw verify -P integration-test
```

WireMock server configuration per test class:
```java
@EnableWireMock({
    @ConfigureWireMock(name = "car-api-mock",    property = "feign.client.config.car-api.url"),
    @ConfigureWireMock(name = "client-api-mock", property = "feign.client.config.client-api.url"),
    @ConfigureWireMock(name = "sales-api-mock",  property = "feign.client.config.sales-api.url"),
    @ConfigureWireMock(name = "keycloak-mock",   property = "feign.client.config.keycloak.url")
})
```

### Mutation Tests (PITest)

PITest runs during the `verify` phase. It requires unit tests to be passing first.

```bash
./mvnw verify -P mutation-test
```

PITest report: `target/pit-reports/index.html`

Coverage thresholds: **90% mutation** / **90% line**. The build fails if either threshold is not met.

### JaCoCo Coverage Report

```bash
./mvnw verify
open target/site/jacoco/index.html
```

Thresholds: **90% instruction** / **90% branch**. The build fails if not met.

---

## Project Structure

```
src/
└── main/
    └── java/br/com/dealership/dealershibff/
        ├── DealershiBffApplication.java       ← main class, @EnableFeignClients
        ├── config/
        │   ├── AsyncConfig.java               ← virtual thread executor
        │   ├── CacheConfig.java               ← Redis TTL configuration
        │   ├── OpenApiConfig.java             ← Springdoc schemas
        │   ├── ResilienceConfig.java          ← aspect order properties
        │   └── SecurityConfig.java            ← Spring Security, RolesClaimConverter
        ├── controller/
        │   ├── AuthController.java
        │   ├── InventoryController.java
        │   ├── ProfileController.java
        │   └── PurchaseController.java
        ├── domain/
        │   └── ErrorCode.java                 ← enum: CAR_NOT_AVAILABLE, VALIDATION_ERROR, ...
        ├── dto/
        │   ├── request/                       ← LoginRequest, RegisterRequest, PurchaseRequest, ...
        │   └── response/                      ← ApiResponse, ApiErrorResponse, VehicleResponse, ...
        ├── feign/
        │   ├── car/
        │   │   ├── CarApiClient.java
        │   │   ├── CarApiFeignConfig.java      ← ErrorDecoder + timeouts for car-api
        │   │   └── dto/                       ← CarApiCarResponse, CarApiPageResponse, ...
        │   ├── client/
        │   │   ├── ClientApiClient.java
        │   │   ├── ClientApiFeignConfig.java
        │   │   └── dto/
        │   ├── keycloak/
        │   │   ├── KeycloakClient.java
        │   │   ├── KeycloakFeignConfig.java
        │   │   └── dto/
        │   └── sales/
        │       ├── SalesApiClient.java
        │       ├── SalesApiFeignConfig.java
        │       └── dto/
        ├── service/
        │   ├── AuthService.java
        │   ├── InventoryService.java
        │   ├── ProfileService.java
        │   └── PurchaseService.java
        └── web/
            ├── GlobalExceptionHandler.java    ← @RestControllerAdvice
            ├── RequestIdFilter.java           ← MDC requestId population
            ├── RequestLoggingFilter.java      ← method/path/status/latency log
            ├── InputSanitizationFilter.java   ← CPF/CEP/phone/q parameter sanitization
            └── TokenRefreshFilter.java        ← transparent refresh token handling

src/
└── test/
    └── java/
        ├── br/com/dealership/dealershibff/    ← Unit tests (Surefire)
        │   ├── controller/
        │   ├── service/
        │   └── web/
        └── integrated/                        ← Integration tests (Failsafe)
            ├── auth/
            ├── inventory/
            ├── profile/
            ├── purchase/
            ├── resilience/
            └── security/
```

---

## Key Configuration Properties

```properties
# Server
server.port=8084

# Downstream service URLs (externalize via env vars in production)
feign.client.config.car-api.url=${CAR_API_BASE_URL:http://localhost:8081}
feign.client.config.car-api.connect-timeout=2000
feign.client.config.car-api.read-timeout=5000

feign.client.config.client-api.url=${CLIENT_API_BASE_URL:http://localhost:8082}
feign.client.config.client-api.connect-timeout=2000
feign.client.config.client-api.read-timeout=5000

feign.client.config.sales-api.url=${SALES_API_BASE_URL:http://localhost:8083}
feign.client.config.sales-api.connect-timeout=2000
feign.client.config.sales-api.read-timeout=5000

feign.client.config.keycloak.url=${KEYCLOAK_BASE_URL:http://localhost:8080}
feign.client.config.keycloak.connect-timeout=3000
feign.client.config.keycloak.read-timeout=8000

# Security - Keycloak JWKS URI
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=${KEYCLOAK_BASE_URL}/realms/${KEYCLOAK_REALM}/protocol/openid-connect/certs

# Redis
spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}

# Cache TTL
spring.cache.redis.time-to-live=300000

# Virtual threads
spring.threads.virtual.enabled=true

# Resilience4j aspect order (Retry=1 outermost → Bulkhead=5 innermost)
resilience4j.retry.retry-aspect-order=1
resilience4j.circuitbreaker.circuit-breaker-aspect-order=2
resilience4j.ratelimiter.rate-limiter-aspect-order=3
resilience4j.timelimiter.time-limiter-aspect-order=4
resilience4j.bulkhead.bulkhead-aspect-order=5

# Actuator
management.endpoints.web.exposure.include=health,info
management.endpoint.health.probes.enabled=true
```

---

## Swagger UI

Available at `/swagger-ui.html` when the application is running. All request/response envelope schemas are registered as OpenAPI components.

---

## Common Issues

### Port conflicts
If port 8084 is in use:
```bash
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Dserver.port=8099"
```

### Redis not running
If Redis is not available, the BFF will start but cache operations will fail. Start Redis:
```bash
docker run -d -p 6379:6379 redis:7-alpine
```

### Keycloak JWKS not reachable
The BFF will fail to start if the JWKS endpoint is unreachable. Ensure Keycloak is running and `KEYCLOAK_BASE_URL` / `KEYCLOAK_REALM` are correct.

### Integration tests Docker requirement
Integration tests require Docker for the Redis Testcontainer. Ensure Docker daemon is running before executing `./mvnw verify -P integration-test`.
