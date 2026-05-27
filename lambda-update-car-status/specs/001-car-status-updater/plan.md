# Implementation Plan: Car Status Updater Lambda

**Branch**: `001-car-status-updater` | **Date**: 2026-05-21 | **Spec**: `D:/JV/Projetos/dealership-ai/lambda-update-car-status/specs/001-car-status-updater/spec.md`
**Input**: Feature specification from `/specs/001-car-status-updater/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

Implement a Go AWS Lambda (provided.al2023, arm64) that consumes SQS sale events, extracts only `SaleId` and `CarId`, and updates the Car API status to `Sold` with strict idempotency semantics (`409` = success). The design uses cold-start dependency wiring (config validation, Secrets Manager secret retrieval, token cache provider, HTTP clients with explicit timeouts, and a reused `sony/gobreaker/v2` circuit breaker), a strict transient/permanent error taxonomy for SQS retry/DLQ behavior, and structured JSON logs with redaction. Testing strategy enforces unit + integration coverage for auth refresh, breaker states, timeout behavior, and handler classification with `>=90%` non-generated code coverage.

## Technical Context


**Language/Version**: Go `1.25` (latest stable approved in `go.mod`)  
**Primary Dependencies**: `github.com/aws/aws-lambda-go`, `github.com/aws/aws-sdk-go-v2` (+ Secrets Manager module), `github.com/sony/gobreaker/v2`, New Relic Lambda Extension Layer `3.x` (layer-only; no business-code SDK calls)  
**Storage**: N/A (stateless Lambda; in-memory token/secret cache for warm instance lifetime only)  
**Testing**: `go test` + `httptest`, table-driven tests, integration tests via real SQS event JSON payloads, `go test -race`, `go test -coverprofile` gate (`>=90%`)  
**Target Platform**: AWS Lambda `provided.al2023` on `arm64`; artifact is Linux arm64 `bootstrap` only  
**Project Type**: Backend serverless event consumer (single Lambda)  
**Performance Goals**: For warm invocations with local integration doubles, target p95 single-message processing duration <= `1000ms`; always fast-fail on open breaker and never use in-function business retries  
**Constraints**: `GOOS=linux GOARCH=arm64 CGO_ENABLED=0`; zip contains only `bootstrap`; required env keys fail-fast at cold start; explicit outbound HTTP timeouts; context propagation across all I/O boundaries  
**Scale/Scope**: Single bounded context: SQS sale event -> Car API `Sold` update with idempotent semantics and operational controls

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] Scope remains bounded to SQS sale-event consumption and Car API `Sold` status
      updates only; no HTTP server/database/event publishing additions.
- [x] Data contract is explicit and minimal (extract `CarId` + `SaleId` only) with
      idempotent handling for duplicate/"already sold" outcomes (e.g., `409`).
- [x] Auth design uses Keycloak client credentials, AWS Secrets Manager at cold
      start, token cache/refresh strategy, and permanent-failure handling after refresh.
- [x] Error taxonomy is defined (transient vs permanent), with `%w` propagation,
      no silent errors, and no custom business retry loops beyond SQS redrive.
- [x] Resilience controls are defined: explicit HTTP timeouts, configurable circuit
      breaker thresholds, context propagation, and goroutine lifecycle discipline.
- [x] Observability outputs are defined: structured JSON logs including `sale_id`,
      `car_id`, outcome category, downstream status, duration, and no secrets/PII.
- [x] Architecture keeps handler thin with dependency injection of HTTP/token/secrets/
      breaker clients initialized at cold start.
- [x] Test/quality plan covers required unit + integration doubles, idempotency,
      error classification, token refresh, timeout/circuit-breaker behavior, >=90%
      core coverage, race detection, lint, and vulnerability scan gates.

**Initial Gate Assessment (Pre-Phase 0): PASS**  
No constitutional violations identified. No unresolved clarifications remain for planning.

## Project Structure

### Documentation (this feature)

```text
specs/001-car-status-updater/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
cmd/lambda/
└── main.go                    # Entrypoint + cold-start wiring only

internal/
├── config/
│   ├── config.go              # Env load + validation (required keys)
│   └── config_test.go
├── handler/
│   ├── sqs_handler.go         # Thin SQS delegate layer
│   └── sqs_handler_test.go
├── service/
│   ├── car_status_service.go  # Business logic + classification
│   └── car_status_service_test.go
├── client/
│   ├── car_api_client.go      # Car API caller (bearer token + breaker)
│   ├── keycloak_client.go     # OAuth2 client-credentials + cache/refresh
│   ├── secrets_client.go      # Secrets Manager retrieval at cold start
│   └── *_test.go
└── observability/
    └── logger.go              # Structured JSON logging helpers/redaction

tests/
├── contract/
│   └── us1_patch_status_contract_test.go
├── integration/
│   └── handler_integration_test.go
└── fixtures/
    └── sqs_sale_event.json

build/
└── package.sh                 # GOOS/GOARCH/CGO build + bootstrap zip checks
```

**Structure Decision**: Adopt a Go Lambda layout with `cmd/lambda` for entrypoint and `internal/{handler,service,client,config}` separation to enforce single responsibility and testability. Runtime clients/config are created once at cold start and injected downward.

## Phase 0 Research Output

Research decisions are documented in `D:/JV/Projetos/dealership-ai/lambda-update-car-status/specs/001-car-status-updater/research.md` and resolve all clarifications.

## Phase 1 Design Output

- Data model: `D:/JV/Projetos/dealership-ai/lambda-update-car-status/specs/001-car-status-updater/data-model.md`
- Contracts: `D:/JV/Projetos/dealership-ai/lambda-update-car-status/specs/001-car-status-updater/contracts/event-and-http-contracts.md`
- Quickstart: `D:/JV/Projetos/dealership-ai/lambda-update-car-status/specs/001-car-status-updater/quickstart.md`

## Constitution Check (Post-Design Re-Check)

- [x] Scope remains bounded to SQS consumer -> Car API `Sold` update only.
- [x] Data contract restricted to `CarId` and `SaleId`; `409` mapped to success/idempotent ack.
- [x] Keycloak client-credentials + Secrets Manager cold-start fetch + token cache/refresh are explicitly designed.
- [x] Error taxonomy includes required transient/permanent mappings and no local business retry loops.
- [x] Timeouts, circuit breaker, and context propagation are mandatory in contract/design artifacts.
- [x] Structured JSON logs schema includes required fields and explicit redaction rules.
- [x] Thin handler and cold-start dependency injection are enforced by package boundaries.
- [x] Tests include required unit/integration scenarios and >=90% coverage gate.

**Post-Design Gate Assessment: PASS**

## Complexity Tracking

No constitutional exceptions identified for this feature.
