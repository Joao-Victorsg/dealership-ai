# Phase 0 Research — Car Status Updater Lambda

## Decision 1: Runtime, build, and artifact model
- **Decision**: Use Go `1.25` (from `go.mod`) and deploy as Lambda custom runtime `provided.al2023` on `arm64`; build with `GOOS=linux GOARCH=arm64 CGO_ENABLED=0`, produce `bootstrap`, and publish zip containing only `bootstrap`.
- **Rationale**: Meets platform constraint exactly; static binary reduces runtime dependency drift and improves cold-start predictability.
- **Alternatives considered**:
  - Managed Go runtime: rejected because requirement mandates `provided.al2023`.
  - x86_64 artifact: rejected because requirement mandates arm64.

## Decision 2: Required dependency set and observability boundary
- **Decision**: Use only mandated core runtime deps for feature behavior: `aws-lambda-go`, `aws-sdk-go-v2` (Secrets Manager), `sony/gobreaker/v2`; use New Relic Lambda Extension layer `3.x` only, with zero New Relic SDK calls in business code.
- **Rationale**: Satisfies mandatory dependency and observability constraints while preserving vendor-neutral application code.
- **Alternatives considered**:
  - `aws-sdk-go` v1: rejected (explicitly forbidden).
  - In-process New Relic SDK instrumentation: rejected by requirement.

## Decision 3: Package architecture and dependency injection
- **Decision**: Enforce package split: `main` (entrypoint/wiring), `handler` (SQS delegate), `service` (business logic), `client` (Car API + Keycloak + secret retrieval), `config` (env read/validate).
- **Rationale**: Aligns with constitutional thin-handler rule and Go testability practices through clear interfaces and injection at cold start.
- **Alternatives considered**:
  - Monolithic handler file: rejected due to low testability and boundary leakage.

## Decision 4: Configuration and fail-fast startup
- **Decision**: Treat all required env values as mandatory and fail Lambda initialization on missing/empty/invalid values.
- **Rationale**: Prevents unsafe runtime defaults and enforces Article XI fail-fast behavior.
- **Alternatives considered**:
  - Lazy per-invocation config errors: rejected due to delayed fault detection.

## Decision 5: Secret retrieval and token lifecycle
- **Decision**: Retrieve Keycloak client secret from Secrets Manager during cold start, cache for runtime instance lifetime, never log secret/token; implement OAuth2 client-credentials token cache with proactive refresh before expiry and no per-invocation forced refresh.
- **Rationale**: Minimizes external auth overhead, supports warm reuse, and meets security constraints.
- **Alternatives considered**:
  - Env-var secret storage: rejected as non-compliant.
  - Refresh every invocation: rejected as unnecessary latency and auth pressure.

## Decision 6: HTTP policy and authentication propagation
- **Decision**: Apply explicit request timeouts for Keycloak and Car API clients and include `Authorization: Bearer <token>` in every Car API request.
- **Rationale**: Required by resilience constraints and ensures consistent downstream auth behavior.
- **Alternatives considered**:
  - Default `http.Client` timeout behavior: rejected (zero timeout risk).

## Decision 7: Circuit breaker strategy
- **Decision**: Wrap all Car API calls in a single cold-start-initialized `gobreaker/v2` instance; breaker settings read from config; open breaker returns immediate transient failure without downstream call.
- **Rationale**: Protects downstream and Lambda resources under repeated failures.
- **Alternatives considered**:
  - Per-request breaker construction: rejected (state loss and overhead).
  - No breaker: rejected by constitutional and feature requirements.

## Decision 8: Error taxonomy and idempotent mapping
- **Decision**: Classify outcomes as:
  - **Transient**: HTTP `429/502/503/504`, network timeout/connection failures, breaker open.
  - **Permanent**: HTTP `400/401/403/404`, malformed/missing `CarId` or `SaleId`, persistent auth failure after refresh.
  - **Success**: HTTP `2xx` and `409 already sold`.
- **Rationale**: Directly maps to SQS retry vs terminal/DLQ behavior and idempotency requirements.
- **Alternatives considered**:
  - Treat `409` as failure: rejected because duplicates/already-sold must ack successfully.

## Decision 9: Logging schema and redaction
- **Decision**: Emit structured JSON logs for each message containing `sale_id`, `car_id`, `outcome`, `duration_ms`, and downstream HTTP status when called; redact secrets/tokens and avoid sensitive payload dumps.
- **Rationale**: Meets observability and security requirements while enabling incident triage.
- **Alternatives considered**:
  - Plain text logs: rejected (poor machine parsing).

## Decision 10: Test plan and quality gates
- **Decision**: Implement unit tests with `httptest` for Keycloak and Car API behaviors (including near-expiry token refresh) and integration tests through handler with real SQS event payload fixtures; include scenarios for breaker opening, breaker-open fast fail/no call, and `409` success ack. Enforce `go test -coverprofile` gate `>=90%` non-generated code.
- **Rationale**: Satisfies mandatory quality gates and validates full message flow behavior.
- **Alternatives considered**:
  - Mock-only unit testing without integration path: rejected (insufficient confidence for handler flow and classification).

## Clarification Resolution Status
All technical context clarifications are resolved. No `NEEDS CLARIFICATION` items remain.

