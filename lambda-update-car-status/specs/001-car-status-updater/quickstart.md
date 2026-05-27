# Quickstart — Car Status Updater Lambda

## 1) Prerequisites
- Go version from `go.mod` (`1.25`).
- AWS credentials/profile with access to Secrets Manager (for runtime execution tests).
- Linux-compatible build tooling (or containerized build).

## 2) Required environment variables (fail-fast)
Set all values; empty/missing must fail startup:
- `CAR_API_BASE_URL`
- `CAR_API_TIMEOUT_MS`
- `KEYCLOAK_TOKEN_URL`
- `KEYCLOAK_CLIENT_ID`
- `KEYCLOAK_SECRET_ID` (Secrets Manager id)
- `KEYCLOAK_REFRESH_SKEW_SECONDS`
- `BREAKER_MAX_REQUESTS`
- `BREAKER_INTERVAL_MS`
- `BREAKER_TIMEOUT_MS`
- `BREAKER_READY_TO_TRIP_FAILURES`
- `LOG_LEVEL`

## 3) Build bootstrap artifact
```bash
GOOS=linux GOARCH=arm64 CGO_ENABLED=0 go build -o bootstrap ./cmd/lambda
zip -j lambda.zip bootstrap
```
Validation: `lambda.zip` contains only `bootstrap`.

## 4) Test execution
```bash
go test ./... -race
go test ./... -coverprofile=coverage.out
```
Coverage gate (non-generated core code): `>=90%`.

## 5) Required test scenarios
- Unit (`httptest`):
  - Keycloak token retrieval and proactive near-expiry refresh.
  - Car API authorization header inclusion.
  - HTTP timeout classification as transient.
  - Auth failures persistent after refresh as permanent.
- Integration (handler + real SQS event payload fixture):
  - Breaker opens after configured failures.
  - Breaker-open path fails fast with no downstream call.
  - Car API `409` already sold returns success acknowledgment.

## 6) Runtime behavior checklist
- Secrets fetched once at cold start and cached for instance lifetime.
- Token cached across warm invocations; no forced per-invocation refresh.
- Circuit breaker wraps every Car API call.
- Structured JSON log per message includes `sale_id`, `car_id`, `outcome`, `duration_ms`, and downstream status when applicable.
- Logs never expose secrets or bearer tokens.

