# Tasks: Car Status Updater Lambda

**Input**: Design documents from `/specs/001-car-status-updater/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Included, because spec requires unit + integration coverage and >=90% coverage gate.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Align repository scaffolding, fixtures, and build/test entrypoints with the plan.

- [X] T001 Create Lambda entrypoint skeleton and dependency wiring placeholders in `cmd/lambda/main.go`
- [X] T002 Add required runtime dependencies (`aws-lambda-go`, `aws-sdk-go-v2`, `gobreaker/v2`) in `go.mod`
- [X] T041 [P] Add and validate `golangci-lint` repository configuration in `.golangci.yml`
- [X] T003 [P] Add/update canonical SQS sale event fixture in `tests/fixtures/sqs_sale_event.json`
- [X] T004 [P] Create contract/integration test package scaffolding in `tests/contract/.keep` and `tests/integration/.keep`
- [X] T005 Create Linux arm64 bootstrap packaging script with zip-content check in `build/package.sh`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Implement mandatory runtime foundations that block all user stories.

**⚠️ CRITICAL**: Complete this phase before starting user-story implementation.

- [X] T006 Implement fail-fast env config loader with required `CAR_API_BASE_URL`, `KEYCLOAK_TOKEN_URL`, `KEYCLOAK_CLIENT_ID`, `KEYCLOAK_SECRET_ID`, and breaker/timeout fields in `internal/config/config.go`
- [X] T007 [P] Add config validation tests for missing/invalid required env (including `CAR_API_BASE_URL`, `KEYCLOAK_TOKEN_URL`, `KEYCLOAK_CLIENT_ID`, `KEYCLOAK_SECRET_ID`) in `internal/config/config_test.go`
- [X] T044 [P] Add explicit fail-fast tests for invalid/missing timeout and breaker env values (`CAR_API_TIMEOUT_MS`, `BREAKER_TIMEOUT_MS`, `BREAKER_MAX_REQUESTS`, `BREAKER_READY_TO_TRIP_FAILURES`) in `internal/config/config_test.go`
- [X] T008 Implement structured JSON logger with secret/token redaction helpers in `internal/observability/logger.go`
- [X] T009 Define shared transient/permanent error taxonomy types and wrappers in `internal/service/errors.go`
- [X] T010 Implement Secrets Manager client to fetch Keycloak secret by `KEYCLOAK_SECRET_ID` at cold start in `internal/client/secrets_client.go`
- [X] T011 [P] Implement Keycloak client-credentials token provider with in-memory cache hooks in `internal/client/keycloak_client.go`
- [X] T012 [P] Implement Car API client using fixed `PATCH` status update and shared circuit breaker wrapper in `internal/client/car_api_client.go`
- [X] T013 Wire cold-start initialization (config, secrets, token provider, HTTP clients, breaker, handler) in `cmd/lambda/main.go`
- [X] T014 Add foundational client tests for timeout propagation, auth bootstrap, and breaker wiring in `internal/client/client_foundation_test.go`

**Checkpoint**: Foundational runtime is ready; user stories can proceed.

---

## Phase 3: User Story 1 - Update car status from sale event (Priority: P1) 🎯 MVP

**Goal**: Process valid sale events and update car status to `Sold`, treating `409` already-sold as success.

**Independent Test**: Send valid SQS event with `SaleId` and `CarId`; verify one `PATCH` request sets `Sold` and message is acknowledged, including `409` idempotent success.

### Tests for User Story 1

- [X] T015 [P] [US1] Add contract test for valid event -> Car API `PATCH /cars/{carId}/status` with `{"status":"Sold"}` in `tests/contract/us1_patch_status_contract_test.go`
- [X] T016 [P] [US1] Add integration test for happy-path `2xx` and idempotent `409` acknowledgment in `tests/integration/us1_success_idempotent_integration_test.go`

### Implementation for User Story 1

- [X] T017 [P] [US1] Create inbound event and outbound status request models (`SaleId`, `CarId`, `Sold`) in `internal/service/models.go`
- [X] T018 [US1] Implement thin SQS handler parsing and delegation to service in `internal/handler/sqs_handler.go`
- [X] T019 [US1] Implement car-status service success path and `409`=>success mapping in `internal/service/car_status_service.go`
- [X] T020 [P] [US1] Add service unit tests for `2xx` and `409` success behavior in `internal/service/car_status_service_test.go`
- [X] T021 [US1] Add SQS handler unit tests for successful batch acknowledgment behavior in `internal/handler/sqs_handler_test.go`

**Checkpoint**: US1 is independently functional and testable.

---

## Phase 4: User Story 2 - Handle duplicate and failed deliveries safely (Priority: P1)

**Goal**: Ensure idempotent duplicate handling and strict transient/permanent failure classification for SQS retry/DLQ behavior.

**Independent Test**: Replay duplicates and inject timeout, malformed payload, and unrecoverable auth failures; verify classification and SQS partial batch failure behavior.

### Tests for User Story 2

- [X] T022 [P] [US2] Add unit tests for transient/permanent classifier matrix (`429/502/503/504`, timeouts, malformed payload, auth failure) in `internal/service/classifier_test.go`
- [X] T023 [P] [US2] Add integration test for duplicate replay consistency and queue retry/DLQ signaling in `tests/integration/us2_replay_and_classification_integration_test.go`
- [X] T042 [P] [US2] Add unit test asserting no in-function business retry loop (single downstream call attempt per message path) in `internal/service/car_status_service_test.go`

### Implementation for User Story 2

- [X] T024 [US2] Implement classifier helpers and outcome mapping utilities in `internal/service/classifier.go`
- [X] T025 [US2] Extend service for duplicate-safe processing and terminal/transient result propagation in `internal/service/car_status_service.go`
- [X] T026 [US2] Implement SQS partial batch failure response construction for transient/permanent outcomes in `internal/handler/sqs_handler.go`
- [X] T027 [P] [US2] Add Car API client error mapping and wrapped diagnostic context propagation in `internal/client/car_api_client.go`
- [X] T028 [US2] Add malformed/duplicate fixture variants for integration scenarios in `tests/fixtures/sqs_sale_event_malformed.json` and `tests/fixtures/sqs_sale_event_duplicate.json`

**Checkpoint**: US2 is independently functional and testable.

---

## Phase 5: User Story 3 - Operate securely and observably under strict runtime controls (Priority: P2)

**Goal**: Enforce startup validation, secure secret/token handling, timeout+breaker resilience, and structured redacted observability.

**Independent Test**: Start with missing required env, simulate token near-expiry/refresh failure, and breaker-open downstream instability; verify fail-fast, refresh, short-circuit, and logs.

### Tests for User Story 3

- [X] T029 [P] [US3] Add startup fail-fast tests for invalid secret payload schema validation (`client_secret`) in `internal/client/secrets_client_test.go`
- [X] T043 [P] [US3] Add startup fail-fast tests specifically for malformed endpoint URL formats/schemes in `CAR_API_BASE_URL` and `KEYCLOAK_TOKEN_URL` in `internal/config/startup_validation_test.go`
- [X] T030 [P] [US3] Add token cache refresh and persistent auth-failure tests in `internal/client/keycloak_client_test.go`
- [X] T031 [P] [US3] Add breaker-open and timeout observability integration test in `tests/integration/us3_resilience_observability_integration_test.go`
- [X] T045 [P] [US3] Add integration test asserting outbound dependency call timing fields are logged for Keycloak and Car API calls in `tests/integration/us3_outbound_call_timing_integration_test.go`

### Implementation for User Story 3

- [X] T032 [US3] Implement strict secret payload validation (`client_secret`) with no-log guarantees in `internal/client/secrets_client.go`
- [X] T033 [US3] Implement proactive token refresh-before-expiry logic and permanent auth failure classification in `internal/client/keycloak_client.go`
- [X] T034 [US3] Enforce timeout and breaker settings from env across outbound boundaries in `internal/client/car_api_client.go`
- [X] T035 [US3] Emit required structured per-message fields (`sale_id`, `car_id`, `outcome`, `duration_ms`, `downstream_http_status`) in `internal/observability/logger.go`
- [X] T046 [US3] Emit structured per-outbound-call timing fields (`dependency`, `call_duration_ms`, `http_status`) for Keycloak and Car API in `internal/observability/logger.go` and `internal/client/*.go`
- [X] T036 [US3] Add logger redaction unit tests preventing secret/token leakage in `internal/observability/logger_test.go`

**Checkpoint**: US3 is independently functional and testable.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final hardening, documentation alignment, and quality-gate automation.

- [X] T037 Update feature docs to lock Car API update method as `PATCH` and app runtime input as `KEYCLOAK_SECRET_ID` in `specs/001-car-status-updater/contracts/event-and-http-contracts.md` and `specs/001-car-status-updater/quickstart.md`
- [X] T038 [P] Add CI quality gates for `go test -race`, coverage >=90%, `golangci-lint`, and vuln scan in `.github/workflows/ci.yml`
- [X] T039 [P] Add cross-story regression integration coverage for US1/US2/US3 critical paths in `tests/integration/handler_regression_test.go`
- [X] T040 Normalize module dependencies and checksums after implementation in `go.mod` and `go.sum`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies.
- **Phase 2 (Foundational)**: Depends on Phase 1; blocks all user stories.
- **Phase 3 (US1)**: Depends on Phase 2.
- **Phase 4 (US2)**: Depends on Phase 2; can run in parallel with US1 after foundation.
- **Phase 5 (US3)**: Depends on Phase 2; can run in parallel with US1/US2, but typically sequenced after US1 for MVP.
- **Phase 6 (Polish)**: Depends on completion of selected user stories.

### User Story Dependency Graph

- **US1 (P1)**: Foundation only.
- **US2 (P1)**: Foundation only (independent from US1, integrates shared service/handler paths).
- **US3 (P2)**: Foundation only (independent validation path).

Graph: `Foundation -> {US1, US2, US3} -> Polish`

### Within-Story Execution Rules

- Write tests first and confirm failures.
- Implement models/utilities before service logic.
- Implement service logic before handler integration.
- Complete each story against its independent test criteria.

---

## Parallel Execution Examples

### User Story 1

```bash
# Parallel test authoring
T015 and T016

# Parallel implementation where files do not overlap
T017 and T020
```

### User Story 2

```bash
# Parallel test tasks
T022 and T023

# Parallel implementation in separate files
T024 and T027
```

### User Story 3

```bash
# Parallel test tasks
T029 and T030 and T031

# Parallel implementation in separate files
T032 and T035
```

---

## Implementation Strategy

### MVP First (US1)

1. Complete Phase 1 and Phase 2.
2. Deliver Phase 3 (US1).
3. Validate US1 independently via contract + integration tests.
4. Demo/deploy MVP.

### Incremental Delivery

1. Foundation complete.
2. Deliver US1 (core status update).
3. Deliver US2 (idempotency and failure routing).
4. Deliver US3 (security/resilience/observability controls).
5. Finish with Phase 6 polish and CI gates.

### Parallel Team Plan

1. Team aligns on Phase 1+2.
2. After foundation: split ownership across US1, US2, US3.
3. Merge at regression/CI gate in Phase 6.





