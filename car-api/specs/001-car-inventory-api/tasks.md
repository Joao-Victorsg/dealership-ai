# Tasks: Car Inventory API

**Input**: Design documents from `/specs/001-car-inventory-api/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Tests are REQUIRED by constitution. Unit tests (JUnit 5 + Mockito + Instancio) and
integration tests (Testcontainers) are included. Tests MUST be written and FAIL before implementation.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Single-module Maven project**: `src/main/java/br/com/dealership/car/api/` and `src/test/java/br/com/dealership/car/api/`
- Resources: `src/main/resources/`
- Flyway migrations: `src/main/resources/db/migration/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Add all required dependencies and configure the project for Redis caching, OAuth2 security, AWS S3, and Bean Validation.

- [X] T001 Add production and test dependencies plus AWS BOM to pom.xml — add spring-boot-starter-data-redis, spring-boot-starter-security, spring-boot-starter-oauth2-resource-server, spring-boot-starter-validation, software.amazon.awssdk:s3, software.amazon.awssdk:s3-presigner, org.instancio:instancio-junit (test), org.testcontainers:testcontainers-localstack (test), spring-boot-starter-security-test (test), and AWS SDK v2 BOM in dependencyManagement
- [X] T002 [P] Configure application.properties with virtual threads, JPA validate mode, Flyway, Redis, OAuth2 JWT issuer-uri, S3 bucket/region/endpoint/presigned-url-ttl, and actuator endpoints in src/main/resources/application.properties
- [X] T003 [P] Add Redis service to compose.yaml using redis:7-alpine image on port 6379

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented — database schema, domain model, shared DTOs, exception handling, configuration, and test infrastructure.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

### Batch A — Independent files (all parallelizable)

- [X] T004 [P] Create Flyway migration with car table, constraints, and indexes in src/main/resources/db/migration/V1__create_car_table.sql — per data-model.md schema (UUID PK, all columns, uq_car_vin, check constraints, indexes on status, category, manufacturer, propulsion_type, year, value, composite status+category)
- [X] T005 [P] Create CarStatus enum (AVAILABLE, SOLD, UNAVAILABLE) in src/main/java/br/com/dealership/car/api/entity/CarStatus.java, CarCategory enum (SUV, SEDAN, SPORT, HATCH, PICKUP) in src/main/java/br/com/dealership/car/api/entity/CarCategory.java, and PropulsionType enum (ELECTRIC, COMBUSTION) in src/main/java/br/com/dealership/car/api/entity/PropulsionType.java
- [X] T006 [P] Create ErrorResponse record (timestamp, status, error, message, fieldErrors) in src/main/java/br/com/dealership/car/api/dto/ErrorResponse.java and FieldError record (field, message) in src/main/java/br/com/dealership/car/api/dto/FieldError.java
- [X] T007 [P] Create CarNotFoundException in src/main/java/br/com/dealership/car/api/exception/CarNotFoundException.java, DuplicateVinException in src/main/java/br/com/dealership/car/api/exception/DuplicateVinException.java, and SoldCarModificationException in src/main/java/br/com/dealership/car/api/exception/SoldCarModificationException.java
- [X] T008 [P] Create SecurityConfig with SecurityFilterChain bean — permit GET /api/v1/cars/**, actuator health, OpenAPI/Swagger paths; require ROLE_STAFF or ROLE_ADMIN for POST/PATCH on /api/v1/cars/**; configure OAuth2 resource server JWT in src/main/java/br/com/dealership/car/api/config/SecurityConfig.java
- [X] T009 [P] Create CacheConfig with RedisCacheManager bean — define car-by-id and car-listings cache regions with 24h TTL and JSON serialization in src/main/java/br/com/dealership/car/api/config/CacheConfig.java
- [X] T010 [P] Create S3Config with S3Client and S3Presigner beans configured from application properties (region, endpoint, credentials) in src/main/java/br/com/dealership/car/api/config/S3Config.java
- [X] T011 [P] Create OpenApiConfig with API info (title, version, description) and security scheme for Bearer JWT in src/main/java/br/com/dealership/car/api/config/OpenApiConfig.java
- [X] T012 [P] Update TestcontainersConfiguration with Redis GenericContainer (redis:7-alpine) using @ServiceConnection and LocalStackContainer for S3 integration tests in src/test/java/br/com/dealership/car/api/TestcontainersConfiguration.java

### Batch B — Depend on Batch A

- [X] T013 Create Car JPA entity with all fields per data-model.md — UUID id, model, manufacturingYear, manufacturer, externalColor, internalColor, vin, status (CarStatus), optionalItems (JSONB via @JdbcTypeCode), category (CarCategory), kilometers (BigDecimal), isNew, propulsionType (PropulsionType), listedValue (BigDecimal), imageKey, registrationDate, createdAt, updatedAt with @PrePersist/@PreUpdate in src/main/java/br/com/dealership/car/api/entity/Car.java
- [X] T014 Create GlobalExceptionHandler @RestControllerAdvice — handle MethodArgumentNotValidException (400 with fieldErrors), CarNotFoundException (404), DuplicateVinException (409), SoldCarModificationException (422), and generic Exception (500) in src/main/java/br/com/dealership/car/api/exception/GlobalExceptionHandler.java
- [X] T015 Update TestCarApiApplication to use updated TestcontainersConfiguration in src/test/java/br/com/dealership/car/api/TestCarApiApplication.java

### Batch C — Depend on Batch B

- [X] T016 Create CarRepository extending JpaRepository<Car, UUID> and JpaSpecificationExecutor<Car> with custom query method existsByVin(String vin) in src/main/java/br/com/dealership/car/api/repository/CarRepository.java
- [X] T017 Create CarResponse record DTO with all public car fields (id, model, manufacturingYear, manufacturer, externalColor, internalColor, vin, status, optionalItems, category, kilometers, isNew, propulsionType, listedValue, imageKey, registrationDate) and a static fromEntity(Car) factory method in src/main/java/br/com/dealership/car/api/dto/CarResponse.java

**Checkpoint**: Foundation ready — user story implementation can now begin in parallel.

---

## Phase 3: User Story 1 — Register a New Car (Priority: P1) 🎯 MVP

**Goal**: Authenticated staff members can register new vehicles with full validation — VIN format/uniqueness, year range, km/isNew consistency, value positivity, initial status restriction, and optional image key.

**Independent Test**: Submit valid and invalid car registration requests via POST /api/v1/cars and verify persisted records match input, all validation rules are enforced, registration date is auto-assigned, and duplicate VINs are rejected.

### Tests for User Story 1 (REQUIRED) ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T018 [P] [US1] Write unit tests for car registration in src/test/java/br/com/dealership/car/api/service/CarServiceTest.java — test shouldRegisterCarWhenAllFieldsValid, shouldRejectWhenVinNot17Chars, shouldRejectWhenVinContainsSpecialChars, shouldNormalizeVinToUppercase, shouldRejectWhenYearBelow1886, shouldRejectWhenYearAboveCurrentPlusOne, shouldRejectWhenNewCarHasKilometersAboveZero, shouldRejectWhenUsedCarHasZeroKilometers, shouldRejectWhenListedValueZeroOrNegative, shouldRejectWhenInitialStatusIsSold, shouldRejectWhenVinAlreadyExists, shouldTreatEmptyImageKeyAsNull, shouldAcceptRegistrationWithoutImageKey. Use @ExtendWith(MockitoExtension.class), Instancio for test data, mock CarRepository
- [X] T019 [P] [US1] Write integration tests for POST /api/v1/cars in src/test/java/br/com/dealership/car/api/controller/CarControllerIntegrationTest.java — test valid creation returns 201 with all fields, validation errors return 400 with fieldErrors array, duplicate VIN returns 409, missing auth returns 401, initial status SOLD returns 400. Use @SpringBootTest with Testcontainers, mock JWT with @WithMockUser or SecurityMockMvcRequestPostProcessors

### Implementation for User Story 1

- [X] T020 [P] [US1] Create CreateCarRequest record with Bean Validation annotations (@NotBlank, @NotNull, @Size, @Positive, etc.) for all fields per post-cars.md contract in src/main/java/br/com/dealership/car/api/dto/CreateCarRequest.java
- [X] T021 [US1] Implement CarService.registerCar(CreateCarRequest) — VIN uppercase normalization, custom validations (km/isNew, year range, initial status), check VIN uniqueness via repository, map to Car entity, persist, evict car-listings cache, return CarResponse in src/main/java/br/com/dealership/car/api/service/CarService.java
- [X] T022 [US1] Implement POST /api/v1/cars endpoint in CarController — accept @Valid @RequestBody CreateCarRequest, delegate to CarService, return 201 Created with CarResponse and Location header in src/main/java/br/com/dealership/car/api/controller/CarController.java
- [X] T023 [US1] Write repository integration test for car persistence, VIN unique constraint enforcement, and registrationDate auto-assignment in src/test/java/br/com/dealership/car/api/repository/CarRepositoryIntegrationTest.java

**Checkpoint**: At this point, User Story 1 should be fully functional — staff can register cars with full validation. This is the MVP.

---

## Phase 4: User Story 2 — Browse and View Car Inventory (Priority: P2)

**Goal**: Any unauthenticated visitor can retrieve a paginated list of cars or view the details of a specific car by ID, enabling the dealership catalog experience.

**Independent Test**: Seed inventory data and verify unauthenticated GET requests return correct paginated lists and individual car details with all public attributes.

### Tests for User Story 2 (REQUIRED) ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T024 [P] [US2] Write unit tests for getCarById() and listCars() in src/test/java/br/com/dealership/car/api/service/CarServiceTest.java — test shouldReturnCarWhenFoundById, shouldThrowCarNotFoundExceptionWhenIdDoesNotExist, shouldReturnPaginatedCarList, shouldReturnEmptyPageWhenNoCarsExist, shouldCacheCarById, shouldCacheCarListings. Use Instancio for test data, mock CarRepository
- [X] T025 [P] [US2] Write integration tests for GET /api/v1/cars and GET /api/v1/cars/{id} in src/test/java/br/com/dealership/car/api/controller/CarControllerIntegrationTest.java — test paginated list returns 200 with page metadata, get by ID returns 200 with complete car, non-existent ID returns 404 with error message, empty inventory returns 200 with empty content array, no auth required for GET endpoints

### Implementation for User Story 2

- [X] T026 [US2] Implement getCarById(UUID) with @Cacheable("car-by-id") and listCars(Pageable) with @Cacheable("car-listings") in src/main/java/br/com/dealership/car/api/service/CarService.java
- [X] T027 [US2] Implement GET /api/v1/cars (paginated, default sort registrationDate desc, max size 100) and GET /api/v1/cars/{id} endpoints in src/main/java/br/com/dealership/car/api/controller/CarController.java
- [X] T028 [US2] Add repository integration tests for pagination queries and default ordering in src/test/java/br/com/dealership/car/api/repository/CarRepositoryIntegrationTest.java

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently — cars can be registered and browsed publicly.

---

## Phase 5: User Story 3 — Filter and Sort Inventory (Priority: P3)

**Goal**: Visitors can filter the inventory by status, category, manufacturer, propulsion type, value range, year range, and new/used indicator. Multiple filters combine with AND logic. Results can be sorted by registration date, listed value, or manufacturing year.

**Independent Test**: Seed diverse inventory data and verify each filter narrows results correctly, filters combine as expected, sort orders produce correctly ordered results, and invalid filter ranges return validation errors.

### Tests for User Story 3 (REQUIRED) ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T029 [P] [US3] Write unit tests for filter and sort logic in src/test/java/br/com/dealership/car/api/service/CarServiceTest.java — test shouldFilterByStatus, shouldFilterByCategory, shouldFilterByManufacturer, shouldFilterByPropulsionType, shouldFilterByValueRange, shouldFilterByYearRange, shouldFilterByIsNew, shouldCombineMultipleFilters, shouldRejectWhenMinValueExceedsMaxValue, shouldRejectWhenMinYearExceedsMaxYear. Mock CarRepository with JpaSpecificationExecutor
- [X] T030 [P] [US3] Write integration tests for filter and sort query parameters in src/test/java/br/com/dealership/car/api/controller/CarControllerIntegrationTest.java — test each filter individually, combined filters, sort by registrationDate/listedValue/manufacturingYear ascending and descending, empty results for no-match filters, invalid filter value returns 400

### Implementation for User Story 3

- [X] T031 [US3] Create CarSpecification with static methods building JPA Criteria predicates for each filter (status, category, manufacturer case-insensitive, propulsionType, minValue, maxValue, minYear, maxYear, isNew) in src/main/java/br/com/dealership/car/api/specification/CarSpecification.java
- [X] T032 [US3] Integrate CarSpecification into CarService.listCars() — accept filter parameters, build combined Specification, validate min/max ranges, pass to repository.findAll(Specification, Pageable) with cache key incorporating all parameters in src/main/java/br/com/dealership/car/api/service/CarService.java
- [X] T033 [US3] Add filter (@RequestParam optional) and sort query parameters to GET /api/v1/cars endpoint — status, category, manufacturer, propulsionType, minValue, maxValue, minYear, maxYear, isNew in src/main/java/br/com/dealership/car/api/controller/CarController.java
- [X] T034 [US3] Add repository integration tests for filter queries, combined filters, sort ordering, and index usage verification in src/test/java/br/com/dealership/car/api/repository/CarRepositoryIntegrationTest.java

**Checkpoint**: At this point, User Stories 1, 2, AND 3 should all work — cars can be registered, browsed, filtered, and sorted.

---

## Phase 6: User Story 4 — Update Mutable Car Attributes (Priority: P4)

**Goal**: Authenticated staff can update a car's status, listed value, or image key. Sold cars cannot be modified. Image updates trigger presigned URL generation for direct S3 uploads and deletion of previous images.

**Independent Test**: Register a car, issue update requests for each mutable field, verify changes persist, verify sold car updates are rejected, verify presigned URL generation returns valid S3 upload URLs, and verify immutable field changes are rejected.

### Tests for User Story 4 (REQUIRED) ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T035 [P] [US4] Write unit tests for updateCar() in src/test/java/br/com/dealership/car/api/service/CarServiceTest.java — test shouldUpdateStatusFromAvailableToUnavailable, shouldUpdateStatusFromAvailableToSold, shouldUpdateStatusFromUnavailableToAvailable, shouldRejectUpdateWhenCarIsSold, shouldUpdateListedValue, shouldRejectWhenUpdatedValueZeroOrNegative, shouldUpdateImageKey, shouldRemoveImageKeyWhenEmptyString, shouldEvictCachesOnUpdate, shouldThrowCarNotFoundWhenUpdatingNonexistentCar. Mock CarRepository, mock S3Service for image deletion
- [X] T036 [P] [US4] Write integration tests for PATCH /api/v1/cars/{id} in src/test/java/br/com/dealership/car/api/controller/CarControllerIntegrationTest.java — test status update returns 200, value update returns 200, imageKey update returns 200, sold car update returns 422, non-existent car returns 404, missing auth returns 401, invalid value returns 400, empty body returns 400
- [X] T037 [P] [US4] Write integration tests for POST /api/v1/cars/{id}/image/presigned-url in src/test/java/br/com/dealership/car/api/controller/CarImageControllerIntegrationTest.java — test valid request returns 200 with presignedUrl/objectKey/expiresIn, unsupported content type returns 400, non-existent car returns 404, missing auth returns 401. Use LocalStack S3 via Testcontainers

### Implementation for User Story 4

- [X] T038 [P] [US4] Create UpdateCarRequest record with optional fields (status, listedValue, imageKey) and validation (@Positive for listedValue) in src/main/java/br/com/dealership/car/api/dto/UpdateCarRequest.java
- [X] T039 [P] [US4] Create PresignedUrlRequest record (contentType with validation for allowed MIME types) in src/main/java/br/com/dealership/car/api/dto/PresignedUrlRequest.java and PresignedUrlResponse record (presignedUrl, objectKey, expiresIn) in src/main/java/br/com/dealership/car/api/dto/PresignedUrlResponse.java
- [X] T040 [P] Implement S3Service with generatePresignedPutUrl(UUID carId, String contentType) and deleteObject(String objectKey) methods using AWS SDK v2 S3Presigner and S3Client in src/main/java/br/com/dealership/car/api/service/S3Service.java
- [X] T041 [P] Implement updateCar(UUID id, UpdateCarRequest) in CarService — validate car exists, check not SOLD, apply mutable fields, delete previous S3 image if imageKey changes, persist, evict car-by-id and car-listings caches, return CarResponse in src/main/java/br/com/dealership/car/api/service/CarService.java
- [X] T042 [P] Implement PATCH /api/v1/cars/{id} endpoint accepting @Valid @RequestBody UpdateCarRequest in src/main/java/br/com/dealership/car/api/controller/CarController.java
- [X] T043 [P] Create CarImageController with POST /api/v1/cars/{id}/image/presigned-url endpoint — accept @Valid @RequestBody PresignedUrlRequest, verify car exists, delegate to S3Service, return PresignedUrlResponse in src/main/java/br/com/dealership/car/api/controller/CarImageController.java

**Checkpoint**: At this point, User Stories 1–4 should all work — cars can be registered, browsed, filtered, updated, and images can be uploaded via presigned URLs.

---

## Phase 7: User Story 5 — Enforce Access Control (Priority: P5)

**Goal**: All write operations (POST, PATCH) require authentication with ROLE_STAFF or ROLE_ADMIN. All read operations (GET) are public. Unauthenticated or unauthorized write attempts are rejected with 401/403.

**Independent Test**: Issue write requests without credentials (expect 401), with invalid roles (expect 403), and with valid staff/admin roles (expect success). Verify all GET endpoints work without authentication.

### Tests for User Story 5 (REQUIRED) ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T044 [P] [US5] Write access control integration tests for CarController in src/test/java/br/com/dealership/car/api/controller/CarControllerIntegrationTest.java — test unauthenticated POST returns 401, non-staff/non-admin POST returns 403, unauthenticated PATCH returns 401, non-staff/non-admin PATCH returns 403, unauthenticated GET list returns 200, unauthenticated GET by ID returns 200. Use Spring Security test support with @WithMockUser and jwt() RequestPostProcessor
- [X] T045 [P] [US5] Write access control integration tests for CarImageController in src/test/java/br/com/dealership/car/api/controller/CarImageControllerIntegrationTest.java — test unauthenticated POST presigned-url returns 401, non-staff role POST presigned-url returns 403

### Implementation for User Story 5

- [X] T046 [US5] Verify and harden SecurityConfig role extraction from JWT claims — ensure roles claim mapping works for ROLE_STAFF and ROLE_ADMIN, confirm CSRF disabled for stateless API, confirm CORS policy, add security-related logging in src/main/java/br/com/dealership/car/api/config/SecurityConfig.java

**Checkpoint**: At this point, all 5 user stories should work end-to-end with full access control enforcement.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories — documentation, analysis, performance, and constitution compliance.

- [ ] T047 [P] Verify OpenAPI documentation covers all 5 endpoints with correct request/response schemas, security requirements, and field descriptions at /swagger-ui.html via springdoc-openapi annotations in src/main/java/br/com/dealership/car/api/controller/CarController.java and src/main/java/br/com/dealership/car/api/controller/CarImageController.java
- [ ] T048 Run static analysis gate — execute mvn -B verify, review IDE inspections for new major issues, document findings and remediation tasks per constitution fallback flow
- [ ] T049 [P] Verify performance targets (p95 < 200ms for list/filter with <= 10,000 cars) — confirm DB indexes are used via EXPLAIN ANALYZE, confirm Redis cache hit ratios, confirm pagination max size 100 is enforced
- [ ] T050 Run quickstart.md validation — start infrastructure via docker compose up -d, run ./mvnw spring-boot:run, verify all 5 endpoints per example cURL commands in specs/001-car-inventory-api/quickstart.md
- [ ] T051 Final code review for constitution compliance — confirm testing standards (JUnit 5, Mockito, Instancio, Testcontainers), API consistency (camelCase JSON, UPPER_SNAKE_CASE enums, consistent error format), observability (actuator health, structured logging, micrometer metrics), and engineering standards (Java 25, Spring Boot 4.0.5, Flyway migrations)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion — BLOCKS all user stories
- **User Stories (Phases 3–7)**: All depend on Foundational phase completion
  - User stories can proceed in parallel (if staffed) or sequentially in priority order (P1 → P2 → P3 → P4 → P5)
  - US3 (Filter/Sort) extends US2 (Browse) — recommended to complete US2 first
  - US4 (Update) is independent of US2/US3 but shares CarController
  - US5 (Access Control) validates security across all endpoints — recommended last
- **Polish (Phase 8)**: Depends on all user stories being complete

### User Story Dependencies

- **US1 (P1)**: Can start after Foundational (Phase 2) — No dependencies on other stories
- **US2 (P2)**: Can start after Foundational (Phase 2) — Adds GET endpoints to CarController created in US1, but independently testable
- **US3 (P3)**: Extends US2's GET /api/v1/cars with filter parameters — recommended after US2 but can be developed in parallel with stub data
- **US4 (P4)**: Can start after Foundational (Phase 2) — Adds PATCH to CarController and creates CarImageController independently
- **US5 (P5)**: Can start after Foundational (Phase 2) — Tests security config created in foundational phase, recommended after US1–US4 for complete coverage

### Within Each User Story

- Tests MUST be written and FAIL before implementation
- DTOs before services
- Services before controllers
- Core implementation before integration tests
- Story complete before moving to next priority

### Parallel Opportunities

- **Phase 1**: T002 and T003 can run in parallel (after T001)
- **Phase 2 Batch A**: T004–T012 can all run in parallel (9 tasks)
- **Phase 2 Batch B**: T013, T014, T015 can run in parallel (after Batch A)
- **Phase 2 Batch C**: T016 and T017 can run in parallel (after Batch B)
- **US1**: T018 and T019 (tests) in parallel; then T020 (DTO) in parallel with test review
- **US2**: T024 and T025 (tests) in parallel
- **US3**: T029 and T030 (tests) in parallel
- **US4**: T035, T036, T037 (tests) in parallel; T038, T039 (DTOs) in parallel
- **US5**: T044 and T045 (tests) in parallel
- **Cross-story**: Once foundational is complete, US1 and US4 can run fully in parallel by different developers

---

## Parallel Example: User Story 1

```text
# Launch tests for User Story 1 in parallel:
Task T018: "Unit tests for car registration in src/test/java/.../service/CarServiceTest.java"
Task T019: "Integration tests for POST /api/v1/cars in src/test/java/.../controller/CarControllerIntegrationTest.java"

# Then launch DTO creation (independent file):
Task T020: "CreateCarRequest record in src/main/java/.../dto/CreateCarRequest.java"

# Sequential implementation (same files, dependencies):
Task T021: "CarService.registerCar() in src/main/java/.../service/CarService.java"
Task T022: "POST endpoint in src/main/java/.../controller/CarController.java"
Task T023: "Repository integration test in src/test/java/.../repository/CarRepositoryIntegrationTest.java"
```

## Parallel Example: User Story 4

```text
# Launch all tests in parallel (different files):
Task T035: "Unit tests for updateCar() in CarServiceTest.java"
Task T036: "Integration tests for PATCH in CarControllerIntegrationTest.java"
Task T037: "Integration tests for presigned URL in CarImageControllerIntegrationTest.java"

# Launch DTOs in parallel (different files):
Task T038: "UpdateCarRequest record in UpdateCarRequest.java"
Task T039: "PresignedUrlRequest + PresignedUrlResponse records"

# Sequential implementation:
Task T040: "S3Service" → Task T041: "CarService.updateCar()" → Task T042: "PATCH endpoint" → Task T043: "CarImageController"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001–T003)
2. Complete Phase 2: Foundational (T004–T017) — CRITICAL, blocks all stories
3. Complete Phase 3: User Story 1 (T018–T023)
4. **STOP and VALIDATE**: Test car registration independently via POST /api/v1/cars
5. Deploy/demo if ready — staff can register cars

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (**MVP — car registration**)
3. Add User Story 2 → Test independently → Deploy/Demo (public catalog browsing)
4. Add User Story 3 → Test independently → Deploy/Demo (filtering and sorting)
5. Add User Story 4 → Test independently → Deploy/Demo (updates and image upload)
6. Add User Story 5 → Test independently → Deploy/Demo (access control hardened)
7. Polish → Final validation → Production ready

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1 (register) → User Story 2 (browse) → User Story 3 (filter)
   - Developer B: User Story 4 (update + image) → supports User Story 5 (access control)
3. Stories complete and integrate independently
4. Team collaborates on Polish phase

---

## Notes

- [P] tasks = different files, no dependencies on incomplete tasks
- [Story] label maps task to specific user story for traceability
- Each user story is independently completable and testable
- Verify tests fail before implementing (TDD per constitution)
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Test conventions: @ExtendWith(MockitoExtension.class), camelCase method names (shouldXxxWhenYyy), Instancio for test data, JUnit 5 assertions only, no comments in test code
- All file paths use package `br.com.dealership.car.api` under `src/main/java/` and `src/test/java/`
