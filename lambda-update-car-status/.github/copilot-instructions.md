# lambda-update-car-status Development Guidelines

Auto-generated from all feature plans. Last updated: 2026-05-21

## Active Technologies
- HCL (Terraform), Bash; Go 1.25 tooling for artifact contract checks (002-lambda-infra-localstack)
- Terraform CLI + LocalStack + AWS CLI/`awslocal` + `build/package.sh` + `scripts/localstack/*` validation workflow (002-lambda-infra-localstack)
- AWS managed resources only (SQS, CloudWatch Logs, IAM, Secrets Manager, Lambda); no application datastore changes (002-lambda-infra-localstack)

- Go `1.25` (latest stable approved in `go.mod`) + `github.com/aws/aws-lambda-go`, `github.com/aws/aws-sdk-go-v2` (+ Secrets Manager module), `github.com/sony/gobreaker/v2`, New Relic Lambda Extension Layer `3.x` (layer-only; no business-code SDK calls) (001-car-status-updater)

## Project Structure

```text
src/
tests/
```

## Commands

# Add commands for Go `1.25` (latest stable approved in `go.mod`)

## Code Style

Go `1.25` (latest stable approved in `go.mod`): Follow standard conventions

## Recent Changes
- 002-lambda-infra-localstack: Added [if applicable, e.g., PostgreSQL, CoreData, files or N/A]
- 002-lambda-infra-localstack: Added [if applicable, e.g., PostgreSQL, CoreData, files or N/A]
- 002-lambda-infra-localstack: Refined infra-only Terraform + LocalStack planning context and validation stack.


<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->
