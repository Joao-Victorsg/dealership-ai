#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ESM_FILE="${ROOT_DIR}/infra/localstack/event-source-mapping.tf"
[[ -f "$ESM_FILE" ]] || { echo "FAIL: event-source-mapping.tf not found"; exit 1; }
grep -q 'aws_sqs_queue.source' "$ESM_FILE" || { echo "FAIL: missing queue dependency"; exit 1; }
grep -q 'aws_sqs_queue.dlq' "$ESM_FILE" || { echo "FAIL: missing dlq dependency"; exit 1; }
grep -q 'aws_secretsmanager_secret_version.keycloak' "$ESM_FILE" || { echo "FAIL: missing config/secret dependency"; exit 1; }
grep -q 'aws_lambda_function.main' "$ESM_FILE" || { echo "FAIL: missing lambda dependency"; exit 1; }
grep -q 'enabled[[:space:]]*=[[:space:]]*true' "$ESM_FILE" || { echo "FAIL: mapping must be enabled"; exit 1; }
echo "DEPENDENCY_ORDER_RESULT: PASS"
