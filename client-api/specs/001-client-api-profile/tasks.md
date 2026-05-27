# Tasks: Client API — Customer Profile Management

**Input**: Design documents from `/specs/001-client-api-profile/`  
**Branch**: `001-client-api-profile` | **Date**: 2026-04-19  
**Spec**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md)

---

## Format Legend

- **[P]**: Parallelizable — operates on a different file than other tasks in the same phase
- **[US#]**: User story label — required for all user story phase tasks
- **[SEC]**: Security / privacy task — mandatory per constitution (Articles IV, V, IX)
- **[TEST-IT]**: Integration test task — Testcontainers + real PostgreSQL + WireMock (Article IX)
- **[CACHE]**: Cache-invalidation task — must be coupled with the write it accompanies (Article XI)
- All tasks include the exact file path where work is performed

---

## Phase 1: Setup

**Purpose**: Project baseline — dependencies, properties, database schema, and test infrastructure

- [x] T001 Configure `src/main/resources/application.properties` with datasource URL/username/password (env vars), Redis host/port, OAuth2 JWKS URI, CPF encryption key and HMAC secret (env vars), ViaCEP base URL, Resilience4j circuit-breaker and time-limiter for `viacep` instance, Spring Cache type (`redis`), cache TTL (24 h), Actuator endpoints (`health`, `readiness`, `liveness`), and `spring.jpa.hibernate.ddl-auto=validate` + `spring.jpa.open-in-view=false`
- [x] T002 Write Flyway migration `src/main/resources/db/migration/V1__create_clients_table.sql`: `clients` table per data-model.md with UUID PK `gen_random_uuid()`, `keycloak_id VARCHAR(255) NOT NULL`, `first_name VARCHAR(100)`, `last_name VARCHAR(100)`, `cpf VARCHAR(100)` (AES-256-GCM ciphertext), `cpf_hash VARCHAR(64)` (HMAC-SHA256 hex), `phone_number VARCHAR(20)`, embedded address columns (`postcode VARCHAR(10)`, `street_number VARCHAR(20)`, `street_name VARCHAR(200)`, `city VARCHAR(100)`, `state VARCHAR(2)`, `address_searched BOOLEAN NOT NULL DEFAULT FALSE`), `created_at TIMESTAMP NOT NULL DEFAULT now()`, `deleted_at TIMESTAMP`; unique indexes `uq_clients_keycloak_id` and `uq_clients_cpf_hash`
- [x] T003 Add `br.com.caelum.stella:caelum-stella-bean-validation` (Jakarta EE-compatible) and `org.springframework.cloud:spring-cloud-starter-openfeign` dependencies to `pom.xml`; add `@EnableFeignClients` to `src/main/java/br/com/dealership/clientapi/ClientApiApplication.java`
- [x] T004 [P] Set up IT test infrastructure: create `src/test/java/integrated/container/PostgresContainerDefinition.java`, `RedisContainerDefinition.java`, `WireMockContainerDefinition.java`, and `ViaCepContainerDefinition.java`; create `src/test/java/integrated/EnvironmentInitializer.java` (`ApplicationContextInitializer` that starts all four containers on a shared Docker network and overrides `DATASOURCE_URL`, `REDIS_HOST`, `REDIS_PORT`, `JWKS_URI`, `VIACEP_BASE_URL`); create `src/test/java/integrated/utils/JwtTestUtils.java` (RSA-2048 keypair, `generateTokenFor(String sub, String... roles)` returns RS256-signed JWT with `realm_access.roles` claim, `getWireMockMappingJson()` returns JWKS stub JSON for WireMock); create `src/test/java/integrated/BaseIT.java` (`@SpringBootTest(webEnvironment=RANDOM_PORT)`, `@ActiveProfiles("it")`, `@ContextConfiguration(initializers=EnvironmentInitializer.class)`, `@BeforeEach` truncates `clients` table via `JdbcTemplate`, clears all caches, resets all circuit breakers, configures REST Assured base URI)
- [x] T005 [P] Configure `src/test/java/br/com/dealership/clientapi/TestcontainersConfiguration.java` with `@ServiceConnection` `PostgreSQLContainer` (PostgreSQL 16-alpine) and `GenericContainer` for Redis 7-alpine used by unit-level `@SpringBootTest` tests; add a stub `JwtDecoder` bean to prevent real JWKS lookup during unit tests

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that every user story depends on. No user story can be started until this phase is complete.

**CRITICAL**: Complete T006 before T009 (entity needed by repository). Complete T010 before T011 (converter needed by security config). Complete T017 before T020 (DTO needed by client). Complete T018 before T019 (exception classes needed by handler).

- [x] T006 Create `Client` JPA entity with embedded `Address` in `src/main/java/br/com/dealership/clientapi/entity/Client.java`; `@Entity @Table(name="clients")`; all fields per data-model.md; `@Convert(converter=CpfEncryptionConverter.class)` on `cpf`; `@Column(name="cpf_hash")` on `cpfHash`; Lombok `@Data @Builder(toBuilder=true) @NoArgsConstructor @AllArgsConstructor @ToString(exclude="cpf")` (CPF excluded to prevent PII leakage in logs); `@PrePersist` sets `createdAt = LocalDateTime.now()` if null; inner `@Embeddable Address` class with same Lombok annotations and `static Address from(ViaCepResponse, String postcode, String streetNumber)` factory
- [x] T007 [P] Implement `CpfEncryptionConverter` as `AttributeConverter<String, String>` in `src/main/java/br/com/dealership/clientapi/persistence/CpfEncryptionConverter.java`; AES-256-GCM with random 12-byte IV per encryption; IV prepended to ciphertext before Base64 encoding; 128-bit GCM auth tag; key loaded from `${app.cpf.encryption-key}`; `convertToEntityAttribute` extracts IV, decrypts, validates auth tag (throws `IllegalStateException` on tamper); returns null for null input; never logs plaintext CPF
- [x] T008 [P] Implement `CpfHashUtil` in `src/main/java/br/com/dealership/clientapi/persistence/CpfHashUtil.java`; `@Component` with `@Value("${app.cpf.hmac-secret}")`; `hash(String cpf)` returns lowercase hex-encoded HMAC-SHA256 of plaintext CPF; deterministic — same CPF always yields same hash; never logs plaintext CPF
- [x] T009 Create `ClientRepository` extending `JpaRepository<Client, UUID>` in `src/main/java/br/com/dealership/clientapi/repository/ClientRepository.java`; `Optional<Client> findByKeycloakId(String)`, `Optional<Client> findByCpfHash(String)`, `boolean existsByKeycloakId(String)`, `boolean existsByCpfHash(String)`; no soft-delete JPA filters — inactive-profile enforcement is a service-layer concern
- [x] T010 [P] Implement `KeycloakJwtConverter` in `src/main/java/br/com/dealership/clientapi/security/KeycloakJwtConverter.java`; implements `Converter<Jwt, AbstractAuthenticationToken>`; reads `realm_access.roles` claim (`List<String>`); maps each to `new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())`; returns `JwtAuthenticationToken` with extracted authorities and `jwt.getSubject()` as principal name; returns empty collection if claim absent
- [x] T011 Configure `SecurityConfig` in `src/main/java/br/com/dealership/clientapi/config/SecurityConfig.java`; `@Configuration @EnableMethodSecurity`; `SecurityFilterChain`: `.authorizeHttpRequests(auth -> auth.requestMatchers("/actuator/**").permitAll().anyRequest().authenticated())`; `.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakJwtConverter)))`
- [x] T012 [P] Configure `RedisConfig` in `src/main/java/br/com/dealership/clientapi/config/RedisConfig.java`; `@Configuration @EnableCaching`; `RedisCacheManager` bean with `entryTtl(Duration.ofHours(24))`, `StringRedisSerializer` for keys, `GenericJacksonJsonRedisSerializer` for values, `disableCachingNullValues()`; explicit `clients` cache entry configured with 24 h TTL
- [x] T013 [P] Configure `OpenApiConfig` in `src/main/java/br/com/dealership/clientapi/config/OpenApiConfig.java`; `@Configuration`; `OpenAPI` bean with title "Client API", version "1.0.0", contact info; `@SecurityScheme(name="bearerAuth", type=HTTP, scheme=bearer, bearerFormat=JWT)`; `SecurityRequirement` applied globally
- [x] T014 [P] Create request DTO records in `src/main/java/br/com/dealership/clientapi/dto/request/`: `CreateClientRequest` (`@NotBlank keycloakId`, `@NotBlank firstName`, `@NotBlank lastName`, `@NotBlank @ValidCpf cpf`, `@NotBlank @Pattern("+55...") phoneNumber`, `@NotBlank postcode`, `@NotBlank streetNumber`; compact constructor strips all fields via `.strip()`); `UpdateClientRequest` (all nullable; class-level `@ValidAddressFields` constraint rejects payload if exactly one of `postcode`/`streetNumber` is non-null; compact constructor throws `IllegalArgumentException` if all fields null); `UpdateCpfRequest` (`@NotBlank @ValidCpf cpf`); `@ValidCpf` annotation with inner `Validator` using Caelum Stella `CPFValidator`; `@ValidAddressFields` class-level constraint; **`UpdateAddressRequest` is NOT needed** — address fields merged into `UpdateClientRequest`
- [x] T015 [P] Create response DTO records in `src/main/java/br/com/dealership/clientapi/dto/response/`: `AddressResponse` record (postcode, streetNumber, streetName, city, state, addressSearched; `static from(Client.Address)` factory); `ClientResponse` (`@Builder` record: id UUID, firstName, lastName, phoneNumber, AddressResponse address, createdAt, deletedAt; `static from(Client)` factory; **cpf and keycloakId must NOT appear**); `Response<T>` generic envelope (`record Response<T>(T data)` with `static <T> Response<T> of(T)`); `ErrorResponse` (`@Builder` record: Instant timestamp, int status, String error, String message, List<FieldError> fieldErrors); `FieldError` record (field, message; `static of(String, String)`)
- [x] T016 [P] Create `ClientMapper` in `src/main/java/br/com/dealership/clientapi/mapper/ClientMapper.java`; `@Component`; hand-written (no MapStruct); `ClientResponse toResponse(Client)`, `AddressResponse toAddressResponse(Client.Address)`, `Client.Address toAddress(ViaCepResponse, String postcode, String streetNumber)` — `addressSearched=true` on non-null response with all fields populated, `addressSearched=false` otherwise
- [x] T017 [P] Create sealed exception hierarchy in `src/main/java/br/com/dealership/clientapi/exception/`: `sealed class ClientApiException extends RuntimeException permits ClientNotFoundException, DuplicateCpfException, DuplicateKeycloakIdException, ProfileInactiveException`; each permitted subtype has a single `String message` constructor; sealed keyword enforces the closed set of business faults at compile time
- [x] T018 Implement `GlobalExceptionHandler` in `src/main/java/br/com/dealership/clientapi/exception/GlobalExceptionHandler.java`; `@RestControllerAdvice`; `MethodArgumentNotValidException` → 400 `Response.of(ErrorResponse)` with `fieldErrors`; `HttpMessageNotReadableException` (root cause `IllegalArgumentException`) and `IllegalArgumentException` (from `UpdateClientRequest` compact constructor) → 400; `DuplicateCpfException` / `DuplicateKeycloakIdException` / `ProfileInactiveException` → 422; `ClientNotFoundException` → 403 returning literal "Access denied" (never reveals profile existence); `AccessDeniedException` → 403; `ObjectOptimisticLockingFailureException` → 409; generic `Exception` fallback → 500 with log; **no PII, no stack traces in responses**
- [x] T019 [P] Create `ViaCepResponse` record in `src/main/java/br/com/dealership/clientapi/client/dto/ViaCepResponse.java`: fields `cep`, `logradouro`, `localidade`, `uf`, `erro` (Boolean)
- [x] T020 [P] Create `ViaCepFeignClient` interface in `src/main/java/br/com/dealership/clientapi/client/ViaCepFeignClient.java`; `@FeignClient(name="viacep", url="${app.viacep.base-url}")`; `@GetMapping("/ws/{postcode}/json/") ViaCepResponse lookupPostcode(@PathVariable("postcode") String postcode)`; connect timeout 5 000 ms, read timeout 5 000 ms
- [x] T021 Implement `ViaCepClient` wrapper in `src/main/java/br/com/dealership/clientapi/client/ViaCepClient.java`; `@Component @RequiredArgsConstructor`; injects `ViaCepFeignClient`; `@CircuitBreaker(name="viacep", fallbackMethod="lookupPostcodeFallback")` on `lookupPostcode(String)` returning `Optional<ViaCepResponse>`; fallback returns `Optional.empty()`; also returns `Optional.empty()` when `response.erro() == Boolean.TRUE`; never propagates exceptions to callers

**Checkpoint**: Foundation ready — all user story phases can now proceed

---

## Phase 3: User Story 1 — Register a New Client Profile (Priority: P1) MVP

**Goal**: `POST /clients` creates a complete business profile with ViaCEP address resolution. CPF and keycloakId uniqueness enforced. ViaCEP failure never blocks registration.

**Independent Test**: `POST /clients` with valid payload returns `201` with a complete profile containing resolved address data and a non-null `createdAt`.

### Implementation

- [x] T022 [US1] Implement `ClientService.createClient(CreateClientRequest)` in `src/main/java/br/com/dealership/clientapi/service/ClientService.java`; `@Transactional`; `existsByKeycloakId` → `DuplicateKeycloakIdException`; compute `cpfHash` via `CpfHashUtil`; `existsByCpfHash` → `DuplicateCpfException`; call `viaCepClient.lookupPostcode(postcode)` → build `Address` via mapper; build and save `Client`; return `clientMapper.toResponse(saved)`
- [x] T023 [US1] Implement `POST /clients` in `src/main/java/br/com/dealership/clientapi/controller/ClientController.java`; `@RestController @RequestMapping("/clients")`; `@PostMapping` returning `ResponseEntity<Response<ClientResponse>>` status 201 with `Location` header via `ServletUriComponentsBuilder`; `@PreAuthorize("hasRole('CLIENT')")`; `@Valid @RequestBody CreateClientRequest`; `@Operation @Tag(name="Clients")` per `contracts/openapi.yml`

### Tests

- [x] T024 [P] [US1] Unit tests for `ClientService.createClient()` in `src/test/java/br/com/dealership/clientapi/service/ClientServiceTest.java`; `@ExtendWith(MockitoExtension.class)`; Instancio for request fixtures; mock `ClientRepository`, `CpfHashUtil`, `ViaCepClient`, `ClientMapper`; cover: ViaCEP success → `addressSearched=true` saved, ViaCEP failure → profile saved with `addressSearched=false`, `DuplicateKeycloakIdException` when keycloakId exists, `DuplicateCpfException` when cpfHash exists
- [x] T025 [P] [US1] Unit tests for `CpfEncryptionConverter` in `src/test/java/br/com/dealership/clientapi/persistence/CpfEncryptionConverterTest.java`; cover: encrypt→decrypt round-trip, two encryptions of same CPF produce different ciphertexts (IV randomness), tampered ciphertext throws `IllegalStateException`, null input returns null
- [x] T026 [P] [US1] Unit tests for `ViaCepClient` in `src/test/java/br/com/dealership/clientapi/client/ViaCepClientTest.java`; `WireMockExtension` (standalone); build `ViaCepFeignClient` manually with `SpringMvcContract` + `JacksonDecoder`; cover: HTTP 200 valid response → populated `Optional`, `{"erro":true}` → `Optional.empty()`, HTTP 503 → fallback `Optional.empty()`, circuit breaker open → fallback `Optional.empty()`
- [x] T027 [US1] [TEST-IT] Integration tests for `POST /clients` in `src/test/java/integrated/ClientControllerIT.java`; extends `BaseIT`; WireMock stubs for ViaCEP; cover all 6 acceptance scenarios: 201 with resolved address, 201 with `addressSearched=false` when ViaCEP unavailable **+ assert response time < 300 ms** (per SC-001), duplicate CPF → 422, duplicate keycloakId → 422, malformed CPF → 400, malformed phone → 400; verify response body, DB state, and HTTP status

**Checkpoint**: `POST /clients` fully functional and independently testable

---

## Phase 4: User Story 2 — View Own Profile (Priority: P2)

**Goal**: `GET /clients/me` returns the authenticated client's full profile from Redis cache (24 h TTL) with DB fallback. Wrong role or unmatched subject → 403.

**Independent Test**: `GET /clients/me` with a valid ROLE_CLIENT JWT returns the profile whose `keycloakId` matches the token `sub` claim.

### Implementation

- [x] T028 [US2] Implement `ClientService.getMyProfile(String keycloakId)` in `src/main/java/br/com/dealership/clientapi/service/ClientService.java`; `@Transactional(readOnly=true) @Cacheable(cacheNames="clients", key="#keycloakId")`; `findByKeycloakId` → `ClientNotFoundException` (→ 403) if absent; return `clientMapper.toResponse(client)`
- [x] T029 [US2] Implement `GET /clients/me` in `src/main/java/br/com/dealership/clientapi/controller/ClientController.java`; `@GetMapping("/me")`; `@PreAuthorize("hasRole('CLIENT')")`; extract keycloakId from `@AuthenticationPrincipal Jwt jwt` via `jwt.getSubject()`; return 200 `Response<ClientResponse>`

### Tests

- [x] T030 [P] [US2] Unit tests for `ClientService.getMyProfile()` in `src/test/java/br/com/dealership/clientapi/service/ClientServiceTest.java`; cover: profile found → DTO returned, profile absent → `ClientNotFoundException` thrown; verify repository called exactly once
- [x] T031 [US2] [TEST-IT] Integration tests for `GET /clients/me` in `src/test/java/integrated/ClientControllerIT.java`; cover: ROLE_CLIENT matching sub → 200, second identical call served from Redis (cache hit), unauthenticated → 401, ROLE_STAFF → 403
- [x] T032 [P] [US2] [SEC] Security integration tests in `src/test/java/integrated/ClientControllerSecurityIT.java`; extends `BaseIT`; cover: ROLE_CLIENT matching sub → 200, ROLE_CLIENT where no profile exists for sub → 403 (**verify 404 is never returned**), ROLE_STAFF token → 403, ROLE_ADMIN token → 403

**Checkpoint**: `GET /clients/me` works with cache; ownership and role rules validated

---

## Phase 5: User Story 3 — Update Own Profile (Priority: P3)

**Goal**: `PATCH /clients/{id}` updates whitelisted fields. Inactive profiles → 422. Cross-client ROLE_CLIENT → 403. ROLE_SYSTEM bypasses ownership. Cache evicted before return.

**Independent Test**: `PATCH /clients/{id}` with a new phone number returns 200 with the updated value and all other fields unchanged.

### Implementation

- [x] T033 [US3] Implement `ClientService.updateClient(UUID id, UpdateClientRequest request, String requesterKeycloakId)` in `src/main/java/br/com/dealership/clientapi/service/ClientService.java`; `@Transactional`; find by ID → `ClientNotFoundException` (→ 403) if absent; `deletedAt != null` → `ProfileInactiveException` (→ 422); if caller has ROLE_CLIENT authority (check via `SecurityContextHolder.getContext().getAuthentication()`, not ROLE_SYSTEM) enforce ownership → `ClientNotFoundException` (→ 403); apply non-null fields via `client.toBuilder()`; if `request.postcode()` non-null, call ViaCEP and rebuild address; save; `cacheManager.getCache("clients").evict(client.getKeycloakId())` BEFORE return; return response [CACHE]
- [x] T034 [US3] Implement `PATCH /clients/{id}` in `src/main/java/br/com/dealership/clientapi/controller/ClientController.java`; `@PatchMapping("/{id}")`; `@PreAuthorize("hasRole('CLIENT') or hasRole('SYSTEM')")`; extract `requesterKeycloakId` from JWT; return 200 `Response<ClientResponse>`; `@Operation` per contract

### Tests

- [x] T035 [P] [US3] Unit tests for `ClientService.updateClient()` in `src/test/java/br/com/dealership/clientapi/service/ClientServiceTest.java`; mock `SecurityContextHolder`; Instancio fixtures; cover: personal-fields-only, address + ViaCEP success → `addressSearched=true`, address + ViaCEP failure → `addressSearched=false`, inactive → `ProfileInactiveException`, ROLE_CLIENT mismatch → `ClientNotFoundException`, ROLE_SYSTEM bypasses ownership; verify cache eviction on every success path
- [x] T036 [US3] [TEST-IT] Integration tests for `PATCH /clients/{id}` in `src/test/java/integrated/ClientControllerIT.java`; cover all US3 scenarios: personal fields only → 200, address + ViaCEP success → `addressSearched=true`, address + ViaCEP failure → `addressSearched=false`, postcode without streetNumber → 400, read-only fields silently ignored, cross-client ROLE_CLIENT → 403, inactive → 422; verify DB state and Redis cache miss after each mutation
- [x] T037 [P] [US3] [SEC] Security integration tests in `src/test/java/integrated/ClientControllerSecurityIT.java`; `PATCH /clients/{id}`: ROLE_CLIENT own → 200, ROLE_CLIENT cross → 403, ROLE_SYSTEM any client → 200, ROLE_STAFF → 403, ROLE_ADMIN → 403, no token → 401

**Checkpoint**: Profile self-service updates work end-to-end with ownership and liveness checks validated

---

## Phase 6: User Story 4 — Administrator CPF Correction (Priority: P4)

**Goal**: `PATCH /clients/{id}/cpf` allows ROLE_ADMIN to correct a client's CPF. Validated and uniqueness-checked via `cpf_hash`. No other role may call this. Cache evicted.

**Independent Test**: ROLE_ADMIN corrects CPF; `cpf_hash` column value changed in DB after the call.

### Implementation

- [x] T038 [US4] Implement `ClientService.correctCpf(UUID id, UpdateCpfRequest request)` in `src/main/java/br/com/dealership/clientapi/service/ClientService.java`; `@Transactional`; find by ID → `ClientNotFoundException` (→ 403) if absent; compute new `cpfHash`; `existsByCpfHash(newHash)` → `DuplicateCpfException` (→ 422); update both `cpf` and `cpfHash` via `toBuilder()`; save; `cacheManager.getCache("clients").evict(client.getKeycloakId())` BEFORE return [CACHE]
- [x] T039 [US4] Implement `PATCH /clients/{id}/cpf` in `src/main/java/br/com/dealership/clientapi/controller/ClientController.java`; `@PatchMapping("/{id}/cpf")`; `@PreAuthorize("hasRole('ADMIN')")`; return 200 `Response<ClientResponse>`; `@Operation` per contract

### Tests

- [x] T040 [P] [US4] Unit tests for `ClientService.correctCpf()` in `src/test/java/br/com/dealership/clientapi/service/ClientServiceTest.java`; use `ArgumentCaptor<Client>` to verify both `cpf` and `cpfHash` updated; cover: success, duplicate CPF → `DuplicateCpfException`, not found → `ClientNotFoundException`
- [x] T041 [US4] [TEST-IT] Integration tests for `PATCH /clients/{id}/cpf` in `src/test/java/integrated/ClientControllerIT.java`; cover all 3 US4 scenarios: ROLE_ADMIN success (verify `cpf_hash` column changed), duplicate CPF → 422, invalid CPF → 400
- [x] T042 [P] [US4] [SEC] Security integration tests in `src/test/java/integrated/ClientControllerSecurityIT.java`; cover: ROLE_ADMIN → 200, ROLE_CLIENT → 403, ROLE_STAFF → 403, ROLE_SYSTEM → 403, no token → 401; primary guard against privilege escalation on CPF correction endpoint

**Checkpoint**: CPF correction is admin-only and all role combinations validated

---

## Phase 7: User Story 5 — Request Account Deletion (Priority: P5)

**Goal**: `DELETE /clients/{id}` anonymizes all PII in place, records `deletedAt`, evicts cache entry, returns 204. Profile record retained. All subsequent mutations → 422.

**Independent Test**: `DELETE /clients/{id}` returns 204; every personal column is replaced with a non-identifiable value; `deleted_at` is non-null.

### Implementation

- [x] T043 [US5] Implement `ClientService.deleteClient(UUID id, String requesterKeycloakId)` in `src/main/java/br/com/dealership/clientapi/service/ClientService.java`; `@Transactional`; find by ID → `ClientNotFoundException` (→ 403) if absent; `deletedAt != null` → `ProfileInactiveException` (→ 422); check ownership → `ClientNotFoundException` (→ 403); capture `originalKeycloakId` BEFORE anonymization; replace all PII: `firstName="ANONYMIZED"`, `lastName="ANONYMIZED"`, `phoneNumber="ANONYMIZED"`, `cpf=UUID.randomUUID().toString()` (converter encrypts), `cpfHash=UUID.randomUUID().toString()`, `keycloakId=UUID.randomUUID().toString()`, all address string fields `=""`, `addressSearched=false`, `deletedAt=LocalDateTime.now()`; save; `cacheManager.getCache("clients").evict(originalKeycloakId)` BEFORE return [CACHE]
- [x] T044 [US5] Implement `DELETE /clients/{id}` in `src/main/java/br/com/dealership/clientapi/controller/ClientController.java`; `@DeleteMapping("/{id}")`; `@PreAuthorize("hasRole('CLIENT')")`; return `ResponseEntity<Void>` status 204; `@Operation` per contract

### Tests

- [x] T045 [P] [US5] Unit tests for `ClientService.deleteClient()` in `src/test/java/br/com/dealership/clientapi/service/ClientServiceTest.java`; `ArgumentCaptor<Client>` to verify every PII field is anonymized and `deletedAt` non-null; verify `cacheManager.getCache("clients").evict(originalKeycloakId)` called; cover: already-deleted → `ProfileInactiveException`, ownership mismatch → `ClientNotFoundException`
- [x] T046 [US5] [TEST-IT] Integration tests for `DELETE /clients/{id}` in `src/test/java/integrated/ClientControllerIT.java`; cover all 4 US5 scenarios: 204 + DB row has anonymized fields, subsequent `PATCH` → 422, cross-client delete → 403, Redis GET on original keycloakId → cache miss after deletion
- [x] T047 [P] [US5] [SEC] Security integration tests in `src/test/java/integrated/ClientControllerSecurityIT.java`; cover: ROLE_CLIENT own → 204, ROLE_CLIENT cross → 403, ROLE_STAFF → 403, ROLE_ADMIN → 403, no token → 401

**Checkpoint**: Account deletion is fully anonymizing, cache-consistent, and role-guarded

---

## Phase 8: User Story 6 — Address Retry (Priority: P6)

**Goal**: `PATCH /clients/{id}` with a ROLE_SYSTEM token retries ViaCEP lookup for clients with `addressSearched=false`. Service method from US3 (T033/T034) handles this path — only dedicated tests added here.

**Independent Test**: Create profile with `addressSearched=false`; invoke `PATCH /clients/{id}` with ROLE_SYSTEM JWT (mismatched sub) while ViaCEP is available; verify `addressSearched=true` in response.

### Tests

- [x] T048 [US6] [TEST-IT] Integration tests for address-retry path of `PATCH /clients/{id}` in `src/test/java/integrated/ClientControllerIT.java`; cover both US6 scenarios: ROLE_SYSTEM + ViaCEP success → all address fields populated + `addressSearched=true`; ROLE_SYSTEM + ViaCEP failure → `addressSearched=false`; **confirm ROLE_SYSTEM sub does not match `client.keycloakId`** (ownership bypass verified)
- [x] T049 [P] [US6] [SEC] Security integration tests for ROLE_SYSTEM in `src/test/java/integrated/ClientControllerSecurityIT.java`; cover: ROLE_SYSTEM mismatched sub → 200, ROLE_STAFF → 403, ROLE_ADMIN → 403; verify `PATCH /clients/{id}` accepts exactly ROLE_CLIENT and ROLE_SYSTEM

**Checkpoint**: Address retry path verified; ROLE_SYSTEM ownership bypass confirmed by security tests

---

## Final Phase: Polish & Cross-Cutting Concerns

**Purpose**: Observability, documentation completeness, and constitution compliance verification

- [x] T050 [P] Implement `RequestLoggingFilter` as `OncePerRequestFilter` in `src/main/java/br/com/dealership/clientapi/web/RequestLoggingFilter.java`; `@Component`; log INFO: `"{} {} -> {} ({}ms)"` method, URI, status, latency; **NEVER log request body, Authorization header, CPF, or any PII**
- [x] T051 [P] Verify all `@Operation`, `@ApiResponse`, and `@Tag` annotations in `src/main/java/br/com/dealership/clientapi/controller/ClientController.java` match `contracts/openapi.yml` exactly — operationId, all response codes (200/201/204/400/401/403/422), schema references; confirm `cpf` and `keycloakId` absent from every response body
- [x] T052 Final constitution compliance check: no PII in any log statement, no New Relic SDK imports in `src/main/`, Actuator `health`+`readiness` accessible without authentication, cache eviction fires BEFORE every write/delete return statement, Flyway migration matches data-model.md exactly, `spring.jpa.open-in-view=false` and `ddl-auto=validate` set, sealed exception hierarchy covers all business fault paths, `@ToString(exclude="cpf")` on `Client` entity

---

## Dependencies (Story Completion Order)

```
Phase 1 (Setup)
    └── Phase 2 (Foundational)
            ├── Phase 3 US1 (Register) ← MVP — must ship first
            │       ├── Phase 4 US2 (View)   ─┐
            │       ├── Phase 5 US3 (Update)  ├── independent of each other after US1
            │       ├── Phase 6 US4 (CPF)     │
            │       └── Phase 7 US5 (Delete) ─┘
            │                   └── Phase 8 US6 (Address Retry) ← depends on US3 endpoint
            └── Final Phase (Polish)
```

- **US2, US3, US4, US5** have no inter-story code dependencies — implement in any order after US1.
- **US6** reuses `ClientService.updateClient()` and `PATCH /clients/{id}` from US3. Start US6 tests only after T033/T034 are complete.
- Within each story: service → endpoint → unit tests (can overlap) → integration tests.

---

## Parallel Execution Examples

### Phase 2 parallel batch (after T006 is done):
```
T007 CpfEncryptionConverter
T008 CpfHashUtil
T010 KeycloakJwtConverter         ← all can run in parallel
T012 RedisConfig
T013 OpenApiConfig
T014 Request DTOs
T015 Response DTOs
T016 ClientMapper
T017 Exception hierarchy
T019 ViaCepResponse DTO
T020 ViaCepFeignClient
```

### Within US1 (after T022 + T023):
```
T024 ClientService unit tests
T025 CpfEncryptionConverter tests  ← all can run in parallel
T026 ViaCepClient unit tests
```

### Cross-story parallel (after Phase 2):
```
US2 (T028–T032) + US4 (T038–T042)   ← fully independent
US3 (T033–T037) + US5 (T043–T047)   ← fully independent
```

---

## Implementation Strategy

**MVP Scope**: Phase 1 + Phase 2 + Phase 3 (US1) only.  
Deliverable: A working `POST /clients` endpoint that creates a profile, resolves an address, enforces uniqueness, and is fully tested.

**Incremental Delivery**:
1. **Sprint 1** — Phases 1–3: Profile registration end-to-end (MVP)
2. **Sprint 2** — Phases 4–5: Profile reads and self-service updates
3. **Sprint 3** — Phases 6–7: Admin CPF correction and account deletion
4. **Sprint 4** — Phase 8 + Final: Address retry path and polish

**Total tasks**: 52  
**Parallelizable tasks**: 24 marked `[P]`  
**Integration test tasks (`[TEST-IT]`)**: 6 (T027, T031, T036, T041, T046, T048)  
**Security test tasks (`[SEC]`)**: 7 (T032, T037, T042, T047, T049)  
**Cache-eviction tasks (`[CACHE]`)**: 3 (T033, T038, T043)
