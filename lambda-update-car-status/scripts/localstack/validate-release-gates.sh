#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
"${ROOT_DIR}/scripts/localstack/check-deploy-orchestrator.sh"
"${ROOT_DIR}/scripts/localstack/validate-inventory-conformance.sh"
"${ROOT_DIR}/scripts/localstack/verify-dependency-order.sh"
"${ROOT_DIR}/scripts/localstack/parity-check.sh"
echo "RELEASE_GATES_RESULT: PASS"
