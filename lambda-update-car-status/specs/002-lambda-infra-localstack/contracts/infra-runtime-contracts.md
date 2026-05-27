# Contracts — Infrastructure Runtime and Deployment Interfaces

## A0) IaC Executor Contract (Scope Guard)

### Required behavior
- Infrastructure execution for this feature must use Terraform CLI only.
- Release evidence must reference Terraform command outputs (`fmt`, `validate`, `plan`, `apply`).

### Gate behavior
- Use of OpenTofu or any alternative IaC executor => **hard fail**.

## A) Build Artifact Contract (Mandatory Pre-Deploy)

### Input
- Source: repository root application code (`./cmd/lambda`)
- Build command: `build/package.sh`

### Output requirements
- Generated file: `lambda.zip`
- Zip content: exactly one entry named `bootstrap`
- Binary target: `GOOS=linux GOARCH=arm64 CGO_ENABLED=0`
- Runtime compatibility: Lambda `provided.al2023` + architecture `arm64`

### Gate behavior
- Any mismatch (missing `bootstrap`, extra files, wrong architecture/runtime metadata) => **hard fail**, deployment blocked.

## B) Queue + DLQ + Redrive Contract

### Resource shape
- One source queue (`sale-events` logical role)
- One dead-letter queue (`sale-events-dlq` logical role)
- Source queue redrive policy referencing DLQ ARN

### Required parameters
- `maxReceiveCount` (integer > 0)
- `visibility_timeout_seconds` and retention settings

### Gate behavior
- Missing redrive linkage or incorrect ARN => **hard fail**.

## C) Lambda Runtime Profile Contract

### Required settings
- `runtime = provided.al2023`
- `architectures = [arm64]`
- `handler = bootstrap` (custom runtime bootstrap entrypoint)
- `timeout`, `memory_size`, and environment variables explicitly set

### Gate behavior
- Runtime/arch mismatch with artifact => **hard fail**.

## D) Event Source Mapping Contract

### Required mapping
- Exactly one active mapping from source queue ARN to function ARN
- Mapping state must be `Enabled`
- Batch and window config explicitly declared

### Gate behavior
- Mapping absent/disabled/wrong source => **hard fail**.

## E) IAM Contract (Least Privilege)

### Minimum action domains
- CloudWatch Logs write lifecycle (for function log delivery)
- SQS consume lifecycle on source queue (receive/delete/change visibility/get attributes)
- Secrets Manager read action for required secret identifiers

### Restrictions
- Avoid wildcard action/resource patterns unless justified and documented as exception.

### Gate behavior
- Over-broad policies or missing required permissions => **hard fail**.

## F) Config and Secrets Wiring Contract

### Required runtime keys (non-empty)
- `CAR_API_BASE_URL`
- `CAR_API_TIMEOUT_MS`
- `KEYCLOAK_TOKEN_URL`
- `KEYCLOAK_CLIENT_ID`
- `KEYCLOAK_SECRET_ID`
- `KEYCLOAK_REFRESH_SKEW_SECONDS`
- `BREAKER_MAX_REQUESTS`
- `BREAKER_INTERVAL_MS`
- `BREAKER_TIMEOUT_MS`
- `BREAKER_READY_TO_TRIP_FAILURES`
- `LOG_LEVEL`

### Gate behavior
- Missing/empty required key or unresolved secret identifier => **hard fail**.

## G) New Relic Observability Contract (Layer-Only)

### Required behavior
- Attach New Relic Lambda Extension layer ARN through infrastructure configuration.
- Set required New Relic environment values through infra inputs.
- Do not require New Relic business-code SDK dependency by default.

### Gate behavior
- Missing extension layer attachment when observability required => **hard fail**.

## H) Post-Deploy Smoke and Parity Contract

### Smoke checks (LocalStack)
1. Success path (SC-004): publish exactly 50 valid messages in <=5 minutes; for each message, measure publish timestamp to first invocation timestamp; pass only when >=48/50 invoke within 60 seconds.
2. Controlled failure path: force transient/permanent failure scenario -> message reaches DLQ per redrive policy.

### Observability + redaction checks (same smoke dataset)
- Per-message structured log fields are mandatory for all 50 smoke messages: `sale_id`, `car_id`, `outcome`, `duration`.
- `downstream_http_status` is mandatory for every message where downstream HTTP was attempted.
- Redaction scan must report zero unredacted occurrences of seeded secret/token/PII sentinel values.

### Parity checks
- Compare local and production-targeted definitions for topology, IAM intent, runtime profile, and required config contract.
- Allow only environment-specific account/endpoint differences.

### Gate behavior
- Any failed smoke/parity/observability/redaction check => approval blocked.

## I) Deploy Orchestrator Audit Contract

### Required behavior
- `../devutils/deploy-all.sh` must either:
  1. execute `../lambda-update-car-status/infra/localstack`; or
  2. emit standardized explicit skip record `SKIP_LAMBDA_INFRA_LOCALSTACK:<reason>`.

### Evidence
- Record the observed behavior and command log in `specs/002-lambda-infra-localstack/evidence/orchestrator-audit.md`.

### Gate behavior
- Missing execute evidence and missing standardized skip record => **hard fail**.

## J) Constitutional Release-Gate Contract

### Required checks
- `go test ./...`
- `go test -race ./...`
- `golangci-lint run ./...`
- `govulncheck ./...`

### Gate behavior
- Any failed or omitted constitutional check => **hard fail**.

