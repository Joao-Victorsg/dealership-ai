#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
grep -q 'PARITY_CHECK_RESULT: PASS' "${ROOT_DIR}/scripts/localstack/parity-check.sh"
grep -Eq 'runtime[[:space:]]*=[[:space:]]*"provided\.al2023"' "${ROOT_DIR}/infra/localstack/lambda.tf"
echo "PASS: parity contract"
