# Implementation Plan: Client API — Customer Profile Management

**Branch**: `001-client-api-profile` | **Date**: 2026-04-17 | **Spec**: [spec.md](spec.md)  
**Input**: Feature specification from `/specs/001-client-api-profile/spec.md`

---

## Summary

Build the Client API, a REST service that owns and manages the business profile of every
registered customer in the automotive dealership platform. The service acts as an OAuth2
Resource Server backed by Keycloak, stores profiles in Aurora PostgreSQL (with CPF
encrypted at rest), caches reads in ElastiCache Redis, and resolves addresses
asynchronously via ViaCEP with graceful degradation through a circuit breaker. Soft
deletion is implemented as full field anonymization. Every endpoint requires
authentication; there are no public routes.

---

## Technical Context

**Language/Version**: Java 25  
**Framework**: Spring Boot 4.0.5  
**Primary Dependencies**: Spring MVC, Spring Security + OAuth2 Resource Server, Spring
Data JPA, Spring Data Redis, Spring Validation, Spring Actuator, Spring AOP, Resilience4j 2.3.0,
springdoc-openapi 2.8.8, Flyway, Lombok  
**Storage**: Aurora PostgreSQL (JPA + Flyway migrations) + ElastiCache Redis (cache layer)  
**Testing**: JUnit 5, Mockito, Instancio 5.3.0 (unit), Testcontainers + REST Assured 5.5.0
(integration), WireMock 3.10.0 (ViaCEP stubs)  
**Target Platform**: Linux server (AWS) — REST API, no frontend  
**Project Type**: web-service  
**Performance Goals**: Profile creation < 3s (even with ViaCEP unavailable); GET < 50ms
(cache hit)  
**Constraints**: No PII in logs; CPF encrypted at rest; no Keycloak calls at request time;
no New Relic SDK in application code  
**Scale/Scope**: Single-tenant dealership platform; single microservice

---

## Constitution Check

*GATE: Verified against Client API Constitution v1.0.0 before and after design.*

| # | Gate (from constitution) | Article | Pre-design | Post-design |
|---|--------------------------|---------|------------|-------------|
| 1 | All data access goes through the Client API — no direct DB reads/writes from external services | III | ✅ | ✅ |
| 2 | Authentication delegated to Keycloak; JWT validated locally via JWKS, no runtime Keycloak calls | II | ✅ | ✅ |
| 3 | No client profile data exposed to ROLE_STAFF; ownership enforced by Keycloak subject ID | IV, V | ✅ | ✅ |
| 4 | CPF stored encrypted at rest (AES-256-GCM) with HMAC hash for uniqueness; never logged | IV | ✅ | ✅ |
| 5 | ViaCEP failure does not block profile creation/update; Resilience4j circuit breaker applied | VI | ✅ | ✅ |
| 6 | Immutable fields silently ignored or rejected on standard update endpoints | VII | ✅ | ✅ |
| 7 | All endpoints documented via springdoc-openapi; 400/422/401/403 used correctly per spec | VIII | ✅ | ✅ |
| 8 | Every endpoint has a Testcontainers integration test against real PostgreSQL | IX | ✅ | ✅ |
| 9 | Dedicated security integration tests for ownership checks and role restrictions | IX | ✅ | ✅ |
| 10 | No New Relic SDK calls in application code; Actuator health/readiness endpoints exposed | X | ✅ | ✅ |
| 11 | Redis cache invalidated before response on every write/delete; cache miss falls through to DB | XI | ✅ | ✅ |

**Result**: All gates pass. No constitutional violations. No complexity justification required.

---

## Project Structure

### Documentation (this feature)

```text
specs/001-client-api-profile/
├── plan.md              ← this file
├── research.md          ← Phase 0 output
├── data-model.md        ← Phase 1 output
├── quickstart.md        ← Phase 1 output
├── contracts/
│   └── openapi.yml      ← Phase 1 output
└── tasks.md             ← Phase 2 output (/speckit.tasks — not created here)
```

### Source Code (repository root)

```text
src/
├── main/
│   ├── java/br/com/dealership/clientapi/
│   │   ├── ClientApiApplication.java
│   │   ├── config/
│   │   │   ├── SecurityConfig.java             # OAuth2 Resource Server, @EnableMethodSecurity
│   │   │   ├── RedisConfig.java                # RedisCacheConfiguration, 24h TTL, Jackson serializer
│   │   │   └── OpenApiConfig.java              # springdoc-openapi customization
│   │   ├── controller/
│   │   │   └── ClientController.java           # @RestController, @PreAuthorize per method
│   │   ├── dto/
│   │   │   ├── request/
│   │   │   │   ├── CreateClientRequest.java
│   │   │   │   ├── UpdateClientRequest.java
│   │   │   │   ├── UpdateAddressRequest.java
│   │   │   │   └── UpdateCpfRequest.java
│   │   │   └── response/
│   │   │       ├── ClientResponse.java
│   │   │       └── AddressResponse.java
│   │   ├── entity/
│   │   │   └── Client.java                     # @Entity, embedded Address, @Convert(cpf)
│   │   ├── exception/
│   │   │   ├── ClientNotFoundException.java
│   │   │   ├── DuplicateCpfException.java
│   │   │   ├── DuplicateKeycloakIdException.java
│   │   │   ├── ProfileInactiveException.java
│   │   │   └── GlobalExceptionHandler.java     # @RestControllerAdvice → 400/422/401/403
│   │   ├── mapper/
│   │   │   └── ClientMapper.java
│   │   ├── persistence/
│   │   │   ├── CpfEncryptionConverter.java     # JPA AttributeConverter (AES-256-GCM)
│   │   │   └── CpfHashUtil.java                # HMAC-SHA256 for uniqueness column
│   │   ├── repository/
│   │   │   └── ClientRepository.java           # Spring Data JPA; findByKeycloakId, findByCpfHash
│   │   ├── security/
│   │   │   └── KeycloakJwtConverter.java       # realm_access.roles → ROLE_xxx GrantedAuthority
│   │   ├── service/
│   │   │   └── ClientService.java              # Business logic, @Cacheable/@CacheEvict
│   │   ├── client/
│   │   │   ├── ViaCepClient.java               # RestClient + @CircuitBreaker(name="viacep")
│   │   │   └── dto/ViaCepResponse.java
│   │   └── web/
│   │       └── RequestLoggingFilter.java       # method+path+status+latency, no body
│   └── resources/
│       ├── application.properties
│       └── db/migration/
│           └── V1__create_clients_table.sql
└── test/
    └── java/br/com/dealership/clientapi/
        ├── controller/
        │   ├── ClientControllerIT.java          # Testcontainers + REST Assured, all endpoints
        │   └── ClientControllerSecurityIT.java  # Role + ownership security tests
        ├── service/
        │   └── ClientServiceTest.java           # JUnit 5 + Mockito, all business rules
        ├── client/
        │   └── ViaCepClientTest.java            # WireMock, circuit breaker, fallback
        └── persistence/
            └── CpfEncryptionConverterTest.java  # Encrypt/decrypt round-trip
```

**Structure Decision**: Single Spring Boot project at the repository root. No modules, no
sub-projects. Standard Maven `src/main` / `src/test` layout.

---

## Phase 0: Research Summary

All NEEDS CLARIFICATION items were resolved. Key decisions (full rationale in
[research.md](research.md)):

| Topic | Decision |
|-------|---------|
| CPF encryption | AES-256-GCM via JPA `AttributeConverter`; separate HMAC-SHA256 `cpf_hash` column for uniqueness |
| JWT role extraction | Custom `JwtAuthenticationConverter` reading `realm_access.roles` |
| ViaCEP resilience | Resilience4j `@CircuitBreaker` with fallback returning empty Optional |
| Redis caching | Spring `@Cacheable`/`@CacheEvict` with 24h TTL, Jackson JSON serialization |
| Flyway migrations | Versioned SQL under `db/migration/`; `cpf` as VARCHAR(100), `cpf_hash` as VARCHAR(64) |
| Soft delete | In-place anonymization; `deletedAt` column; service-layer inactive check (not JPA filter) |
| Request logging | Servlet `Filter` capturing method+path+status+latency; no body logged |
| New Relic | Java Agent via `-javaagent` at deploy time; zero SDK imports in code |

---

## Phase 1: Design Artifacts

| Artifact | Location | Status |
|----------|----------|--------|
| Data model | [data-model.md](data-model.md) | ✅ Complete |
| OpenAPI contract | [contracts/openapi.yml](contracts/openapi.yml) | ✅ Complete |
| Quickstart guide | [quickstart.md](quickstart.md) | ✅ Complete |

### API Surface (6 endpoints)

| Method | Path | Role(s) | Description |
|--------|------|---------|-------------|
| `POST` | `/clients` | `ROLE_CLIENT` | Register new profile |
| `GET` | `/clients/me` | `ROLE_CLIENT` | Get own profile (cached) |
| `PATCH` | `/clients/{id}` | `ROLE_CLIENT`, `ROLE_SYSTEM` | Update name/phone/address + ViaCEP re-resolution; `ROLE_CLIENT` ownership enforced; `ROLE_SYSTEM` bypasses ownership |
| `PATCH` | `/clients/{id}/cpf` | `ROLE_ADMIN` | Correct CPF (admin only) |
| `DELETE` | `/clients/{id}` | `ROLE_CLIENT` | Anonymize account (ownership enforced) |

### Post-Design Constitution Re-Check

All gates remain ✅. The 6-endpoint design:
- Exposes zero profile data to `ROLE_STAFF` (no endpoint accepts that role).
- The 403-not-404 policy is enforced at the service layer for all ownership checks.
- `cpf` and `keycloakId` are absent from `ClientResponse` — no PII leak in responses.
- Cache eviction is annotated on all write-path service methods.

---

## Complexity Tracking

No constitutional violations. No complexity justification required.
