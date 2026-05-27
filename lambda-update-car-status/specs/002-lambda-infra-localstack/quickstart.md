# Quickstart — Lambda Infrastructure LocalStack

## 1) Prerequisites
- LocalStack running with Lambda, SQS, Logs, IAM, and Secrets Manager services enabled.
- AWS CLI + `awslocal` wrapper available.
- Terraform CLI available and configured for LocalStack endpoints (`infra/localstack` module).
- Go toolchain (`go 1.25`) for runtime artifact build.

## 2) Build and validate runtime package contract
From repository root:
```bash
build/package.sh
unzip -Z1 lambda.zip
```
Expected output from `unzip -Z1`: only `bootstrap`.

## 3) Configure required runtime inputs
Provide non-empty values for:
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

Also provide New Relic layer/input variables (environment-specific):
- `NEW_RELIC_EXTENSION_LAYER_ARN`
- required New Relic account/license/config environment values per platform policy.

`keycloak_client_secret` in `terraform.tfvars` must be the raw secret value. The infra module writes this to Secrets Manager as JSON payload `{"client_secret":"<value>"}` required by lambda startup.
When using `devutils/keycloak/setup-keycloak.sh`, the local defaults are `keycloak_client_id=lambda-update-car-status`, `keycloak_client_secret=lambda-update-car-status-secret`, and `keycloak_token_url=http://keycloak:8080/realms/dealership/protocol/openid-connect/token` (internal Docker network endpoint expected by Lambda/LocalStack runtime).

## 4) Orchestrator audit gate (`deploy-all.sh`) — mandatory
Before infra apply, verify `../devutils/deploy-all.sh` behavior for this module:
```bash
grep -n "lambda-update-car-status/infra/localstack" ../devutils/deploy-all.sh
```
Pass criteria:
- Script contains and executes the step for `../lambda-update-car-status/infra/localstack`, **or**
- Script emits standardized skip record `SKIP_LAMBDA_INFRA_LOCALSTACK:<reason>` when absent.

Evidence file: `specs/002-lambda-infra-localstack/evidence/orchestrator-audit.md` (mandatory).

## 5) Dependency-ordered infra deploy workflow
From repository root run the sequence exactly as written:
```bash
cp infra/localstack/terraform.tfvars.example infra/localstack/terraform.tfvars
scripts/localstack/check-deploy-orchestrator.sh
scripts/localstack/preflight.sh
scripts/localstack/validate-artifact-contract.sh
terraform -chdir=infra/localstack init
terraform -chdir=infra/localstack fmt -check
terraform -chdir=infra/localstack validate
terraform -chdir=infra/localstack plan -var-file=terraform.tfvars -out=tfplan
terraform -chdir=infra/localstack apply -auto-approve tfplan
scripts/localstack/validate-inventory-conformance.sh
scripts/localstack/verify-dependency-order.sh
```

Required success snippets mapped to the sequence:
- `Terraform has been successfully initialized!` (init)
- `Success! The configuration is valid.` (validate)
- `Apply complete! Resources:` (apply)
- `SMOKE_TEST_RESULT: PASS` (smoke-success)

1. Validate IaC (`fmt/validate/plan`).
2. Apply base resources: log group, DLQ, source queue with redrive, secrets/config resources.
3. Apply IAM role/policies (least privilege).
4. Deploy Lambda function with:
   - `runtime=provided.al2023`
   - `architectures=[arm64]`
   - artifact `lambda.zip`
   - New Relic extension layer attachment.
5. Create/enable event source mapping from source queue to function.

## 6) Post-deploy smoke checks (mandatory)

### Success path
```bash
scripts/localstack/smoke-success.sh \
  --queue-url <SOURCE_QUEUE_URL> \
  --function-name <FUNCTION_NAME> \
  --messages 50 \
  --window-seconds 300 \
  --latency-threshold-seconds 60
```
SC-004 pass criteria:
- Mapping is enabled.
- Exactly 50 valid messages are published in <=5 minutes.
- Publish-to-first-invocation latency is measured per message.
- At least 48/50 messages (96%) invoke within 60 seconds.

Methodology reference: record raw timestamps and computed latency summary in `specs/002-lambda-infra-localstack/evidence/us2-runtime-deploy-validation.md`.

### Observability contract + redaction gate (mandatory)
Validate the same smoke-success dataset with constitutional field and redaction checks:
```bash
scripts/localstack/validate-observability-contract.sh \
  --log-file specs/002-lambda-infra-localstack/evidence/smoke-success-logs.jsonl \
  --required-fields sale_id,car_id,outcome,duration \
  --require-downstream-http-status-when-attempted \
  --redaction-scan
```
Pass criteria:
- 50/50 smoke messages have structured log fields `sale_id`, `car_id`, `outcome`, `duration`.
- Every message with downstream HTTP attempt includes `downstream_http_status`.
- Redaction scan reports `0` unredacted secret/token/PII sentinel values.

### Controlled failure path -> DLQ
Inject a failing payload or controlled downstream failure condition, then verify DLQ receives message after retry policy exhaustion:
```bash
awslocal sqs receive-message --queue-url <DLQ_URL> --max-number-of-messages 1
```
Pass criteria: expected failed message appears in DLQ.

## 7) Parity and security-intent checks (mandatory gate)
Before approval, compare LocalStack and production-targeted definitions:
- topology (one source queue + one DLQ + one mapping)
- runtime profile (`provided.al2023`, `arm64`, bootstrap artifact)
- IAM permission scope (no unjustified wildcards)
- required config/secrets contract
- New Relic extension layer presence (layer-only strategy)

Any drift outside allowed endpoint/account differences blocks progression.

## 8) Constitutional release gates (mandatory before approval)
Execute and archive outputs:
```bash
go test ./...
go test -race ./...
golangci-lint run ./...
govulncheck ./...
```
All commands must pass and be referenced in final evidence (`specs/002-lambda-infra-localstack/evidence/final-validation-log.md`).

## Failure example
Example gate failure:
`Error: Post "http://localhost:4566": dial tcp 127.0.0.1:4566: connect: connection refused`

## Recovery path
1. Start or restore LocalStack, then verify health (`awslocal sqs list-queues`).
2. Re-run validation and apply in order: `preflight -> validate-artifact-contract -> terraform init/fmt/validate/plan/apply -> inventory/dependency checks`.
3. Re-run smoke and confirm evidence includes `SMOKE_TEST_RESULT: PASS`.
4. If two recovery attempts fail, record blocker evidence in `specs/002-lambda-infra-localstack/evidence/final-validation-log.md` and escalate to owner `platform-team`.
