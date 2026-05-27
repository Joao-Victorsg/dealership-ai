#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
checks=(
  "runtime          = \"provided.al2023\"|infra/localstack/lambda.tf"
  "architectures    = [\"arm64\"]|infra/localstack/lambda.tf"
  "resource \"aws_sqs_queue\" \"source\"|infra/localstack/sqs.tf"
  "resource \"aws_sqs_queue\" \"dlq\"|infra/localstack/sqs.tf"
  "aws_lambda_event_source_mapping|infra/localstack/event-source-mapping.tf"
)
for check in "${checks[@]}"; do
  pattern="${check%%|*}"
  file="${check##*|}"
  grep -Fq "$pattern" "${ROOT_DIR}/${file}" || { echo "PARITY_CHECK_RESULT: FAIL (${file} missing ${pattern})"; exit 1; }
done
echo "PARITY_CHECK_RESULT: PASS"
