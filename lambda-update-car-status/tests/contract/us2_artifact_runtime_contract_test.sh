#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
grep -q 'GOOS=linux GOARCH=arm64' "${ROOT_DIR}/build/package.sh"
grep -Eq 'runtime[[:space:]]*=[[:space:]]*"provided\.al2023"' "${ROOT_DIR}/infra/localstack/lambda.tf"
grep -Eq 'architectures[[:space:]]*=[[:space:]]*\["arm64"\]' "${ROOT_DIR}/infra/localstack/lambda.tf"
echo "PASS: artifact runtime contract"
