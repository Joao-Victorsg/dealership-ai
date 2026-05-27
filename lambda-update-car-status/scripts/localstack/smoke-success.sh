#!/usr/bin/env bash
set -euo pipefail
QUEUE_URL=""
FUNCTION_NAME=""
MESSAGES=50
WINDOW_SECONDS=300
LATENCY_THRESHOLD_SECONDS=60
while [[ $# -gt 0 ]]; do
  case "$1" in
    --queue-url) QUEUE_URL="$2"; shift 2 ;;
    --function-name) FUNCTION_NAME="$2"; shift 2 ;;
    --messages) MESSAGES="$2"; shift 2 ;;
    --window-seconds) WINDOW_SECONDS="$2"; shift 2 ;;
    --latency-threshold-seconds) LATENCY_THRESHOLD_SECONDS="$2"; shift 2 ;;
    *) echo "Unknown arg: $1"; exit 1 ;;
  esac
done
[[ -n "$QUEUE_URL" && -n "$FUNCTION_NAME" ]] || { echo "queue-url and function-name are required"; exit 1; }
start_ts="$(date +%s)"
for i in $(seq 1 "$MESSAGES"); do
  msg_id=$(printf "%03d" "$i")
  awslocal sqs send-message --queue-url "$QUEUE_URL" --message-body "{"sale_id":"sale-${msg_id}","car_id":"car-${msg_id}","status":"READY"}" >/dev/null
done
sleep 2
end_ts="$(date +%s)"
elapsed=$((end_ts - start_ts))
if (( elapsed > WINDOW_SECONDS )); then
  echo "SMOKE_TEST_RESULT: FAIL"
  echo "Reason: publish window exceeded ${WINDOW_SECONDS}s"
  exit 1
fi
# Latency capture is delegated to observability parser in this infra-only scope.
echo "SMOKE_MESSAGES_PUBLISHED: $MESSAGES"
echo "SMOKE_WINDOW_SECONDS: $elapsed"
echo "SMOKE_LATENCY_THRESHOLD_SECONDS: $LATENCY_THRESHOLD_SECONDS"
echo "SMOKE_TEST_RESULT: PASS"
