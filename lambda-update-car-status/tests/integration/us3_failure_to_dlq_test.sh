#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
grep -q 'SMOKE_DLQ_RESULT' "${ROOT_DIR}/scripts/localstack/smoke-dlq.sh"
echo "PASS: failure to dlq integration scaffold"
