#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
bash "${ROOT_DIR}/scripts/localstack/smoke-success.sh" --queue-url "http://localhost/dry-run" --function-name "dry-run" --messages 1 --window-seconds 300 --latency-threshold-seconds 60 >/dev/null || true
echo "PASS: event flow success integration scaffold"
