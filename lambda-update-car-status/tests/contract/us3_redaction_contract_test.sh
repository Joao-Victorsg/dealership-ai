#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
grep -q 'REDACTION_VIOLATION_COUNT' "${ROOT_DIR}/scripts/localstack/validate-observability-contract.sh"
grep -q '0 unredacted occurrences' "${ROOT_DIR}/specs/002-lambda-infra-localstack/spec.md"
echo "PASS: redaction contract"
