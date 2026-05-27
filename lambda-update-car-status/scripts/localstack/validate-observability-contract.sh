#!/usr/bin/env bash
set -euo pipefail
LOG_FILE=""
REQUIRED_FIELDS="sale_id,car_id,outcome,duration"
REQUIRE_DOWNSTREAM=0
REDACTION_SCAN=0
while [[ $# -gt 0 ]]; do
  case "$1" in
    --log-file) LOG_FILE="$2"; shift 2 ;;
    --required-fields) REQUIRED_FIELDS="$2"; shift 2 ;;
    --require-downstream-http-status-when-attempted) REQUIRE_DOWNSTREAM=1; shift ;;
    --redaction-scan) REDACTION_SCAN=1; shift ;;
    *) echo "Unknown arg: $1"; exit 1 ;;
  esac
done
[[ -n "$LOG_FILE" ]] || { echo "--log-file is required"; exit 1; }
[[ -f "$LOG_FILE" ]] || { echo "log file not found: $LOG_FILE"; exit 1; }
IFS=',' read -r -a fields <<< "$REQUIRED_FIELDS"
messages=0
missing=0
downstream_attempted=0
downstream_missing=0
redaction_violations=0
while IFS= read -r line; do
  [[ -z "$line" ]] && continue
  messages=$((messages+1))
  for f in "${fields[@]}"; do
    echo "$line" | jq -e --arg f "$f" 'has($f) and .[$f] != null and .[$f] != ""' >/dev/null || missing=$((missing+1))
  done
  attempted=$(echo "$line" | jq -r '.downstream_http_attempted // false')
  if [[ "$attempted" == "true" ]]; then
    downstream_attempted=$((downstream_attempted+1))
    if [[ "$REQUIRE_DOWNSTREAM" -eq 1 ]]; then
      echo "$line" | jq -e 'has("downstream_http_status") and .downstream_http_status != null' >/dev/null || downstream_missing=$((downstream_missing+1))
    fi
  fi
  if [[ "$REDACTION_SCAN" -eq 1 ]]; then
    if echo "$line" | grep -Eqi '(SECRET_VALUE|ACCESS_TOKEN|CPF=|password=|authorization: bearer [^*])'; then
      redaction_violations=$((redaction_violations+1))
    fi
  fi
done < "$LOG_FILE"
[[ "$messages" -gt 0 ]] || { echo "OBSERVABILITY_RESULT: FAIL (no messages)"; exit 1; }
[[ "$missing" -eq 0 ]] || { echo "OBSERVABILITY_RESULT: FAIL (required_fields_missing=$missing)"; exit 1; }
[[ "$downstream_missing" -eq 0 ]] || { echo "OBSERVABILITY_RESULT: FAIL (downstream_http_status_missing=$downstream_missing)"; exit 1; }
[[ "$redaction_violations" -eq 0 ]] || { echo "OBSERVABILITY_RESULT: FAIL (redaction_violation_count=$redaction_violations)"; exit 1; }
echo "OBSERVABILITY_MESSAGES: $messages"
echo "OBSERVABILITY_DOWNSTREAM_ATTEMPTED: $downstream_attempted"
echo "REDACTION_VIOLATION_COUNT: $redaction_violations"
echo "OBSERVABILITY_RESULT: PASS"
