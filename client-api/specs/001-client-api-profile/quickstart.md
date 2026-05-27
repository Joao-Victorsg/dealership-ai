# Quickstart: Client API — Customer Profile Management

**Feature**: `001-client-api-profile`  
**Date**: 2026-04-17

---

## Prerequisites

| Tool | Version | Purpose |
|------|---------|---------|
| Java | 25 | Runtime and compilation |
| Maven | 3.9+ | Build and dependency management |
| Docker | 24+ | Local Redis (compose.yaml) + Testcontainers |
| PostgreSQL | N/A (Testcontainers) | Integration tests only |

---

## 1. Local Development Setup

### Start Redis

The project ships with a `compose.yaml` at the repository root that starts Redis:

```bash
docker compose up -d
```

This starts a Redis container on port 6379 (mapped dynamically). Spring Boot DevTools
will detect it automatically via `spring-boot-docker-compose`.

### Database (Development)

For local development, spin up a PostgreSQL container manually or point
`spring.datasource.url` at an existing instance. Flyway migrations run automatically
on startup.

```bash
docker run -d \
  --name client-api-db \
  -e POSTGRES_DB=clientdb \
  -e POSTGRES_USER=clientapi \
  -e POSTGRES_PASSWORD=secret \
  -p 5432:5432 \
  postgres:16
```

Then configure `application-local.properties` (not committed):

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/clientdb
spring.datasource.username=clientapi
spring.datasource.password=secret
spring.security.oauth2.resourceserver.jwt.jwks-uri=http://localhost:8080/realms/dealership/protocol/openid-connect/certs
client-api.encryption.secret-key=<32-byte-hex-key>
client-api.encryption.hmac-secret=<32-byte-hex-secret>
```

### Run the Application

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

---

## 2. Running Tests

### Unit Tests Only

```bash
./mvnw test -pl . -Dtest="**/*Test"
```

### Integration Tests (Testcontainers + PostgreSQL)

Testcontainers downloads and starts a PostgreSQL container automatically. Docker must
be running.

```bash
./mvnw verify
```

This runs both unit tests and integration tests (`**/*IT`).

---

## 3. API Documentation

With the application running locally, OpenAPI UI is available at:

```
http://localhost:8080/swagger-ui.html
```

The raw OpenAPI JSON spec:

```
http://localhost:8080/v3/api-docs
```

---

## 4. New Relic Agent (Observability)

The New Relic Java Agent is **not bundled in this repository**. It is attached at
runtime by the deployment layer. For local development, observability is handled via
Spring Boot Actuator only.

Actuator health endpoints:

```
GET /actuator/health
GET /actuator/health/readiness
GET /actuator/health/liveness
```

---

## 5. Key Configuration Properties Reference

| Property | Description | Example |
|----------|-------------|---------|
| `spring.datasource.url` | JDBC URL for Aurora PostgreSQL | `jdbc:postgresql://host:5432/clientdb` |
| `spring.datasource.username` | DB username | `clientapi` |
| `spring.datasource.password` | DB password | `secret` |
| `spring.data.redis.host` | Redis host | `localhost` |
| `spring.data.redis.port` | Redis port | `6379` |
| `spring.security.oauth2.resourceserver.jwt.jwks-uri` | Keycloak JWKS endpoint | `https://keycloak/realms/dealership/protocol/openid-connect/certs` |
| `client-api.encryption.secret-key` | AES-256 key (32-byte hex) | `a1b2c3...` |
| `client-api.encryption.hmac-secret` | HMAC-SHA256 secret (32-byte hex) | `d4e5f6...` |
| `resilience4j.circuitbreaker.instances.viacep.sliding-window-size` | ViaCEP CB window | `10` |
| `resilience4j.circuitbreaker.instances.viacep.failure-rate-threshold` | ViaCEP CB threshold | `50` |
| `resilience4j.circuitbreaker.instances.viacep.wait-duration-in-open-state` | ViaCEP CB open wait | `30s` |
| `client-api.viacep.base-url` | ViaCEP API base URL | `https://viacep.com.br/ws` |

---

## 6. Project Structure Overview

```
src/
├── main/
│   ├── java/br/com/dealership/clientapi/
│   │   ├── ClientApiApplication.java
│   │   ├── config/
│   │   │   ├── SecurityConfig.java           # OAuth2 Resource Server + role converter
│   │   │   ├── RedisConfig.java              # Cache TTL and serialization
│   │   │   └── OpenApiConfig.java            # springdoc-openapi configuration
│   │   ├── controller/
│   │   │   └── ClientController.java         # All REST endpoints
│   │   ├── dto/
│   │   │   ├── request/                      # CreateClientRequest, UpdateClientRequest, etc.
│   │   │   └── response/                     # ClientResponse, AddressResponse
│   │   ├── entity/
│   │   │   └── Client.java                   # JPA entity with embedded Address
│   │   ├── exception/
│   │   │   ├── ClientNotFoundException.java
│   │   │   ├── DuplicateCpfException.java
│   │   │   ├── DuplicateKeycloakIdException.java
│   │   │   ├── ProfileInactiveException.java
│   │   │   └── GlobalExceptionHandler.java   # @RestControllerAdvice
│   │   ├── mapper/
│   │   │   └── ClientMapper.java             # Entity ↔ DTO conversion
│   │   ├── persistence/
│   │   │   ├── CpfEncryptionConverter.java   # JPA AttributeConverter (AES-256-GCM)
│   │   │   └── CpfHashUtil.java              # HMAC-SHA256 for cpf_hash column
│   │   ├── repository/
│   │   │   └── ClientRepository.java         # Spring Data JPA
│   │   ├── security/
│   │   │   └── KeycloakJwtConverter.java     # realm_access.roles → GrantedAuthority
│   │   ├── service/
│   │   │   └── ClientService.java            # Business logic + cache annotations
│   │   ├── client/
│   │   │   ├── ViaCepClient.java             # HTTP client + @CircuitBreaker
│   │   │   └── dto/ViaCepResponse.java
│   │   └── web/
│   │       └── RequestLoggingFilter.java     # Method+path+status+latency (no body)
│   └── resources/
│       ├── application.properties
│       └── db/migration/
│           └── V1__create_clients_table.sql
└── test/
    └── java/br/com/dealership/clientapi/
        ├── controller/
        │   ├── ClientControllerIT.java        # Testcontainers + real PostgreSQL
        │   └── ClientControllerSecurityIT.java # Security + ownership tests
        ├── service/
        │   └── ClientServiceTest.java         # JUnit 5 + Mockito unit tests
        ├── client/
        │   └── ViaCepClientTest.java          # Circuit breaker + fallback tests
        └── persistence/
            └── CpfEncryptionConverterTest.java
```
