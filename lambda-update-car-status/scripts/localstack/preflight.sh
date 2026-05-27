#!/usr/bin/env bash
set -euo pipefail
required_tools=(terraform awslocal jq)
for tool in "${required_tools[@]}"; do
  command -v "$tool" >/dev/null 2>&1 || { echo "Missing required tool: $tool"; exit 1; }
done
awslocal sqs list-queues >/dev/null 2>&1 || {
  echo "LocalStack preflight failed: unable to reach SQS endpoint"
  exit 1
}
echo "PREFLIGHT_RESULT: PASS"
