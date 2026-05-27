# Feature Specification: Car Status Updater Lambda

**Feature Branch**: `001-car-status-updater`  
**Created**: 2026-05-21  
**Status**: Draft  
**Input**: User description: "Produce or update the feature specification for Car Status Updater Lambda with constitution v1.1.0 constraints"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Update car status from sale event (Priority: P1)

As an operations stakeholder, I need each valid sale event to update the corresponding car status to `Sold` so downstream inventory reflects completed sales.

**Why this priority**: This is the core business outcome and the only in-scope responsibility of the Lambda.

**Independent Test**: Send a valid sale event containing `CarId` and `SaleId`; confirm the car status is updated to `Sold` and the message is acknowledged.

**Acceptance Scenarios**:

1. **Given** a valid sale event with `CarId` and `SaleId`, **When** the message is processed, **Then** the system requests the car status transition to `Sold` and records a successful outcome.
2. **Given** a valid sale event for a car already marked `Sold`, **When** the message is processed, **Then** the system treats the downstream “already target state” response as successful completion.

---

### User Story 2 - Handle duplicate and failed deliveries safely (Priority: P1)

As a platform owner, I need at-least-once message processing to be idempotent and correctly classified so transient failures can retry via queue redrive while permanent failures move toward DLQ handling.

**Why this priority**: SQS delivery semantics require safe duplicate handling and reliable failure routing.

**Independent Test**: Replay the same event and inject failure types (timeout, malformed payload, auth failure) to verify classification, propagation, and acknowledgment behavior.

**Acceptance Scenarios**:

1. **Given** the same sale event is delivered multiple times, **When** each delivery is processed, **Then** the resulting car state remains consistent and no conflicting side effects occur.
2. **Given** a transient downstream failure (for example timeout, `429`, or `503`), **When** processing occurs, **Then** the message is returned as failed for queue-managed retry without in-function retry loops.
3. **Given** a permanent failure (for example malformed payload or unrecoverable auth/config error), **When** processing occurs, **Then** the message is marked as terminal behavior for DLQ path according to queue policy.

---

### User Story 3 - Operate securely and observably under strict runtime controls (Priority: P2)

As an SRE/security stakeholder, I need startup validation, secret/token handling, circuit protection, timeouts, and structured logs so incidents are diagnosable and unsafe runtime behavior is prevented.

**Why this priority**: Reliability and compliance requirements are constitutional constraints for production readiness.

**Independent Test**: Start the function with missing required config, expired token conditions, and downstream instability; verify fail-fast behavior, token cache/refresh behavior, circuit breaker outcomes, timeout enforcement, and structured logs.

**Acceptance Scenarios**:

1. **Given** required configuration or secret retrieval is invalid at cold start, **When** initialization runs, **Then** startup fails fast and processing does not begin.
2. **Given** a cached token is near expiry, **When** a message requires downstream access, **Then** the token is refreshed before expiry and reused across warm invocations.
3. **Given** repeated downstream failures trigger the breaker threshold, **When** subsequent messages arrive while circuit is open, **Then** calls are short-circuited with controlled errors and observable structured logs.

---

### Edge Cases

- Sale event missing `CarId` and/or `SaleId` must be classified as permanent failure.
- Secret source reachable at cold start but missing required auth fields must fail startup.
- Token refresh failure after cache expiry must be treated as permanent auth failure.
- Outbound call exceeding configured timeout must be classified as transient failure.
- Circuit breaker open state during valid message processing must skip downstream call and surface controlled failure.
- Logging must redact tokens, secrets, and any sensitive identifiers while still enabling triage.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST keep bounded responsibility to consuming sale events and requesting car status update to `Sold`; it MUST NOT introduce unrelated workflow orchestration, event publication, database ownership, or HTTP server behavior.
- **FR-002**: The system MUST accept only the event contract fields `CarId` and `SaleId` as business-driving inputs for this feature.
- **FR-003**: The system MUST implement idempotent processing for SQS at-least-once semantics, including treating duplicate deliveries and already-target-state downstream responses as successful completion.
- **FR-004**: The system MUST retrieve authentication secrets from the configured secret source during cold start and MUST NOT hardcode credentials or tokens.
- **FR-005**: The system MUST use client-credentials authentication with token caching across warm invocations and proactive refresh before expiry.
- **FR-006**: The system MUST classify auth failures that persist after refresh attempt as permanent failures.
- **FR-007**: The system MUST classify failures into transient and permanent categories, with transient failures returned for queue-managed retry and permanent failures treated as terminal for DLQ path.
- **FR-008**: The system MUST NOT execute custom in-function business retry loops for downstream update attempts.
- **FR-009**: The system MUST enforce circuit breaker protection for downstream car update calls with configurable thresholds and explicit short-circuit behavior when open.
- **FR-010**: The system MUST enforce strict outbound timeout controls for each external call boundary.
- **FR-011**: The system MUST produce structured logs for every processed message including `sale_id`, `car_id`, outcome category, downstream status when available, and processing duration.
- **FR-012**: The system MUST prevent secrets and sensitive token values from appearing in logs.
- **FR-013**: The system MUST fail fast at cold start when required configuration values are missing or invalid, including endpoint, secret identifier, timeout values, and circuit breaker thresholds.
- **FR-014**: The system MUST keep handler logic thin and delegate business behavior to injected dependencies created at cold start.
- **FR-015**: The system MUST provide automated test coverage of at least 90% for non-generated core application code, including unit and integration tests covering idempotency, token cache/refresh, error taxonomy, timeout behavior, and circuit breaker behavior.

### Operational & Quality Requirements *(mandatory for backend/services)*

- **OQ-001**: Bounded context is enforced: SQS sale event consumer -> Car API status update to `Sold` only.
- **OQ-002**: Event contract and idempotency semantics are explicitly documented for `CarId`, `SaleId`, duplicates, and already-sold responses.
- **OQ-003**: Auth/secret flow includes startup secret retrieval, warm token cache reuse, and pre-expiry refresh.
- **OQ-004**: Error taxonomy defines transient vs permanent classes and propagation to SQS/DLQ behavior without local retry loops.
- **OQ-005**: Timeout and circuit-breaker policies are mandatory, configurable, and applied at all outbound boundaries.
- **OQ-006**: Structured logging schema is mandatory for each message, with redaction controls for secrets/PII.
- **OQ-007**: Quality gates require unit and integration tests with controlled doubles and >=90% core coverage.

### Key Entities *(include if feature involves data)*

- **Sale Status Event**: Message consumed from queue containing the minimal business identifiers (`SaleId`, `CarId`) required to process a status update.
- **Car Status Update Request**: Outbound status transition request representing intent to set one car to `Sold`.
- **Auth Session Token**: Cached access credential obtained via client credentials flow, including expiry metadata for refresh decisions.
- **Processing Outcome Record**: Logical result of one message handling attempt including classification (success, transient failure, permanent failure), downstream status, and duration.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of valid sale events containing `CarId` and `SaleId` result in either successful status transition to `Sold` or successful acknowledgment of already-target-state responses.
- **SC-002**: 100% of duplicate event replays for the same `SaleId` and `CarId` preserve consistent final car state without conflicting side effects.
- **SC-003**: 100% of malformed payloads and unrecoverable auth/config failures are classified as permanent outcomes, while timeout/`429`/`503` failures are classified as transient outcomes.
- **SC-004**: 100% of outbound dependency calls execute with configured timeout limits and recorded duration in structured logs.
- **SC-005**: 100% of cold starts with missing/invalid required configuration fail before message processing begins.
- **SC-006**: Automated test suites demonstrate at least 90% coverage on non-generated core application code, including required unit and integration scenarios.

## Assumptions

- Sales events are delivered through a dedicated queue with redrive policy configured externally.
- Car API behavior for already-target-state is stable and returns a distinguishable response (for example conflict semantics) that can be mapped to success.
- Secret source permissions and network connectivity are available at cold start in deployment environments.
- Required configuration keys are centrally managed and provided per environment before deployment.
- Integration tests use controlled doubles for downstream dependencies and do not call production or live external services.

