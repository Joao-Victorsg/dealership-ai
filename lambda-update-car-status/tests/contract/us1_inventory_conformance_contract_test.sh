#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
grep -q '<env>-lambda-update-car-status-<resource-type>-<instance>' "${ROOT_DIR}/specs/002-lambda-infra-localstack/spec.md"
grep -q 'validate-inventory-conformance.sh' "${ROOT_DIR}/scripts/localstack/deploy.sh"
echo "PASS: inventory conformance contract"
