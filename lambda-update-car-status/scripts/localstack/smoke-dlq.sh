#!/usr/bin/env bash
set -euo pipefail
SOURCE_QUEUE_URL=""
DLQ_URL=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --source-queue-url) SOURCE_QUEUE_URL="$2"; shift 2 ;;
    --dlq-url) DLQ_URL="$2"; shift 2 ;;
    *) echo "Unknown arg: $1"; exit 1 ;;
  esac
done
[[ -n "$SOURCE_QUEUE_URL" && -n "$DLQ_URL" ]] || { echo "source-queue-url and dlq-url are required"; exit 1; }
awslocal sqs send-message --queue-url "$SOURCE_QUEUE_URL" --message-body '{"sale_id":"sale-fail-001","car_id":"car-fail-001","force_failure":true}' >/dev/null
sleep 2
awslocal sqs receive-message --queue-url "$DLQ_URL" --max-number-of-messages 1 | jq -e '.Messages | length >= 1' >/dev/null || {
  echo "SMOKE_DLQ_RESULT: FAIL"
  exit 1
}
echo "SMOKE_DLQ_RESULT: PASS"
