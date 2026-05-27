#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SCRIPT="${ROOT_DIR}/scripts/localstack/validate-observability-contract.sh"
grep -q 'sale_id,car_id,outcome,duration' "$SCRIPT"
grep -q 'downstream_http_status' "$SCRIPT"
echo "PASS: structured log fields contract"
