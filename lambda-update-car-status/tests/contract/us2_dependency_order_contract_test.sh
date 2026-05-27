#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SCRIPT="${ROOT_DIR}/scripts/localstack/verify-dependency-order.sh"
FILE="${ROOT_DIR}/infra/localstack/event-source-mapping.tf"
grep -q 'aws_sqs_queue.source' "$FILE"
grep -q 'aws_secretsmanager_secret_version.keycloak' "$FILE"
grep -q 'aws_lambda_function.main' "$FILE"
grep -q 'DEPENDENCY_ORDER_RESULT: PASS' "$SCRIPT"
echo "PASS: dependency order contract"
