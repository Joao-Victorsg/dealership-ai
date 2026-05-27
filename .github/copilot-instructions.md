# Copilot Instructions for `dealership-ai`

## Build, test, and lint commands

Run commands from the target module directory.

### Java services (`car-api`, `client-api`, `sales-api`, `dealership-bff`)

```powershell
# Build + unit + integration tests
.\mvnw.cmd clean verify

# Unit tests only (Surefire)
.\mvnw.cmd test

# Run a single unit test class
.\mvnw.cmd -Dtest=SaleServiceTest test

# Integration tests only (Failsafe, tests in integrated/**)
.\mvnw.cmd failsafe:integration-test

# Run a single integration test class
.\mvnw.cmd -Dit.test=integrated.SaleControllerIT failsafe:integration-test
```

### Web app (`dealership-web`)

```powershell
npm ci
npm run dev
npm run build
npm run lint
npm run type-check
npm run test

# Single Vitest file
npm run test -- tests/unit/lib/api/client.test.ts

# Single Vitest test name
npm run test -- -t "bffFetch"

npm run e2e

# Single Playwright spec
npm run e2e -- e2e/purchase.spec.ts
```

### Go Lambdas (`lambda-update-car-status`, `lambda-start-invoice-workflow`, `lambda-invoice-processor`, `lambda-send-email`)

```powershell
# Full suite
go test ./...

# Single package / test
go test ./internal/handler -run TestHandle

# Build Lambda bootstrap artifact
$env:GOOS="linux"; $env:GOARCH="arm64"; $env:CGO_ENABLED="0"; go build -o bootstrap ./cmd/lambda
```

`lambda-update-car-status` additionally uses:

```powershell
golangci-lint run
go test ./... -race
go test ./... -coverprofile=coverage.out
```

### Local platform dependencies

```powershell
docker compose -f devutils\docker-compose.yml up -d
```

This starts LocalStack, Keycloak (+ Postgres), smtp4dev, and Caddy for local integration flows.

## High-level architecture

- Monorepo with a **Next.js frontend** (`dealership-web`), a **BFF** (`dealership-bff`), three domain APIs (`car-api`, `client-api`, `sales-api`), and Go Lambdas for asynchronous processing.
- Frontend calls BFF only. `dealership-web/lib/api/client.ts` resolves `BFF_URL` (server-side) / `NEXT_PUBLIC_BFF_URL` (client-side) and forwards cookies for session-authenticated calls.
- BFF orchestrates downstream APIs via OpenFeign:
  - `CarApiClient` → `car-api` (`/api/v1/cars...`)
  - `ClientApiClient` → `client-api` (`/clients...`)
  - `SalesApiClient` → `sales-api` (`/api/v1/sales...`)
- Purchase flow:
  1. BFF `PurchaseService` checks car availability and builds client/car snapshots.
  2. BFF sends `Authorization` header + snapshot payload to `sales-api`.
  3. `sales-api` persists sale, applies transaction tax, and publishes `SaleEventPayload` to SNS (`sales-topic`).
- Eventing/workflow flow:
  - `infra-sns` defines `sales-topic`.
  - `infra-sqs` subscribes `car-status-queue` and `invoice-queue` (with DLQs) to that topic.
  - `lambda-update-car-status` consumes `car-status-queue` and PATCHes `car-api` to set `status=SOLD`.
  - `lambda-start-invoice-workflow` consumes `invoice-queue` and starts Step Functions execution.
  - `infra-step-function` orchestrates `lambda-invoice-processor` -> `lambda-send-email`.
    - `lambda-invoice-processor`: renders invoice HTML and uploads to S3.
    - `lambda-send-email`: reads invoice from S3 and sends through SES.
- Infrastructure modules communicate via Terraform remote state (not hard-coded ARNs between modules).

## Key conventions

- **API envelope conventions**:
  - Domain APIs return `Response<T>` with top-level `data`.
  - BFF returns `ApiResponse<T>` with `data` + `meta` (`requestId`, timestamp, pagination metadata).
- **Purchase contract conventions**:
  - BFF->Sales payload includes `clientId`, `clientSnapshot`, and `carSnapshot` (not minimal identifiers only).
  - BFF must forward caller `Authorization` header on sales registration.
- **Testing layout convention (Java)**:
  - Unit tests stay outside `integrated/**`.
  - Integration tests live under `integrated/**` and are executed by Failsafe, not Surefire.
- **Quality gates**:
  - Java modules enforce high coverage/mutation thresholds (commonly 90%).
  - `dealership-web` Vitest coverage thresholds are 90% across lines/branches/functions/statements.
  - `lambda-update-car-status` CI enforces lint + race + coverage threshold checks.
- **Caching convention**:
  - Redis cache keys are namespaced per service (`car-api::`, `client-api::`, `dealership-bff::`) to avoid collisions.
- **Async convention in BFF**:
  - Parallel downstream calls use `CompletableFuture.supplyAsync(..., virtualThreadExecutor)`.
  - `AsyncConfig` propagates MDC and New Relic token context into async tasks.
- **Frontend contract conventions**:
  - BFF client wrapper throws typed `BffError` from standardized error body.
  - Frontend comments/contracts in `lib/api/*.ts` are source-of-truth for endpoint usage and expected envelope shapes.
- **Next.js version caveat**:
  - Follow `dealership-web/AGENTS.md` and `dealership-web/CLAUDE.md`: treat current Next.js as potentially breaking relative to older defaults; check `node_modules/next/dist/docs/` when in doubt.

## Related instruction files

- Per-service instruction files already exist in:
  - `car-api/.github/copilot-instructions.md`
  - `client-api/.github/copilot-instructions.md`
  - `sales-api/.github/copilot-instructions.md`
  - `dealership-bff/.github/copilot-instructions.md`
  - `dealership-web/.github/copilot-instructions.md`

Use this root file for cross-service workflows; use the service file for module-specific details.
