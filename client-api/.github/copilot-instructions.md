# client-api Development Guidelines

Auto-generated from all feature plans. Last updated: 2026-04-17

## Active Technologies

- Java 25  
 + Spring MVC, Spring Security + OAuth2 Resource Server, Spring
 (001-client-api-profile)

## Project Structure

```text
backend/
frontend/
tests/
```

## Commands

# Add commands for Java 25  

## Code Style

Java 25  
: Follow standard conventions

## Recent Changes

- 001-client-api-profile: Added Java 25  
 + Spring MVC, Spring Security + OAuth2 Resource Server, Spring

<!-- MANUAL ADDITIONS START -->

## Full Technology Stack (001-client-api-profile)

| Layer | Technology |
|-------|-----------|
| Language | Java 25 |
| Framework | Spring Boot 4.0.5 |
| Web | Spring MVC (`spring-boot-starter-webmvc`) |
| Security | Spring Security + OAuth2 Resource Server (`realm_access.roles` → `ROLE_xxx`) |
| Persistence | Spring Data JPA + Aurora PostgreSQL + Flyway |
| Cache | Spring Data Redis (ElastiCache) — `@Cacheable`/`@CacheEvict`, 24h TTL |
| Resilience | Resilience4j 2.3.0 — `@CircuitBreaker` on ViaCEP |
| API Docs | springdoc-openapi 2.8.8 |
| Observability | New Relic Java Agent (javaagent only) + Spring Actuator |
| Utilities | Lombok, Instancio 5.3.0, WireMock 3.10.0, REST Assured 5.5.0 |
| Tests | JUnit 5, Mockito, Testcontainers (PostgreSQL + Redis) |

## Project Layout

```text
src/main/java/br/com/dealership/clientapi/
├── config/          SecurityConfig, RedisConfig, OpenApiConfig
├── controller/      ClientController (@PreAuthorize per method)
├── dto/             request/ + response/ DTOs
├── entity/          Client (embedded Address)
├── exception/       Custom exceptions + GlobalExceptionHandler (@RestControllerAdvice)
├── mapper/          ClientMapper
├── persistence/     CpfEncryptionConverter (AES-256-GCM), CpfHashUtil (HMAC-SHA256)
├── repository/      ClientRepository (Spring Data JPA)
├── security/        KeycloakJwtConverter (realm_access.roles)
├── service/         ClientService (business logic + cache)
├── client/          ViaCepClient + dto/ViaCepResponse
└── web/             RequestLoggingFilter (no body logging)

src/main/resources/
├── application.properties
└── db/migration/V1__create_clients_table.sql
```

## Commands

```bash
# Build
./mvnw clean verify

# Run (local profile)
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Unit tests only
./mvnw test

# Integration tests (requires Docker)
./mvnw verify -Pfailsafe

# Start Redis for local dev
docker compose up -d
```

## Code Conventions (this project)

- Roles: `ROLE_CLIENT`, `ROLE_ADMIN`, `ROLE_SYSTEM`. `ROLE_STAFF` has no access.
- Ownership check: JWT `sub` claim must match `client.keycloakId` → 403 on mismatch (never 404).
- CPF: stored as AES-256-GCM ciphertext in `cpf` column; uniqueness via `cpf_hash` (HMAC-SHA256).
- Soft delete: `deletedAt` column; anonymized profiles are permanently inactive.
- Cache key: `client::{uuid}`; evict before returning on every write/delete.
- Unit tests: JUnit 5 + Mockito + Instancio. See `java-tests.instructions.md` for standards.
- Integration tests: Testcontainers with real PostgreSQL. No mocked DB layer.
- No New Relic SDK imports anywhere in `src/main`.
- Request bodies are never logged (PII risk).

<!-- MANUAL ADDITIONS END -->
