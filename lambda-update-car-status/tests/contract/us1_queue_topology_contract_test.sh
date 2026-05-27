#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
FILE="${ROOT_DIR}/infra/localstack/sqs.tf"
grep -q 'aws_sqs_queue" "source' "$FILE"
grep -q 'aws_sqs_queue" "dlq' "$FILE"
grep -q 'redrive_policy' "$FILE"
echo "PASS: queue topology contract"
