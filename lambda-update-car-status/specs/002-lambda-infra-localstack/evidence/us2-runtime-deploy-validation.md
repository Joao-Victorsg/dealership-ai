# US2 Runtime Deploy Validation
## Executed checks
- PASS: `tests/contract/us2_artifact_runtime_contract_test.sh`
- PASS: `tests/contract/us2_structured_log_fields_contract_test.sh`
- PASS: `tests/contract/us2_dependency_order_contract_test.sh`
- PASS: `scripts/localstack/validate-artifact-contract.sh`
- PASS: `scripts/localstack/validate-observability-contract.sh --log-file specs/002-lambda-infra-localstack/evidence/smoke-success-logs.jsonl --required-fields sale_id,car_id,outcome,duration --require-downstream-http-status-when-attempted --redaction-scan`
## Pending runtime checks (LocalStack dependency)
- BLOCKED: SC-004 live smoke (`scripts/localstack/smoke-success.sh`) because `awslocal` is not installed
- BLOCKED: Terraform apply and live event source mapping validation
