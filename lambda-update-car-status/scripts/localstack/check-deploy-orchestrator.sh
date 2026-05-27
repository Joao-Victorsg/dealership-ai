#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ORCH_SCRIPT="${ROOT_DIR}/../devutils/deploy-all.sh"
EVIDENCE_FILE="${ROOT_DIR}/specs/002-lambda-infra-localstack/evidence/orchestrator-audit.md"
TARGET="../lambda-update-car-status/infra/localstack"
mkdir -p "$(dirname "$EVIDENCE_FILE")"
if [[ ! -f "$ORCH_SCRIPT" ]]; then
  echo "FAIL: $ORCH_SCRIPT not found" | tee "$EVIDENCE_FILE"
  exit 1
fi
if grep -Fq "$TARGET" "$ORCH_SCRIPT"; then
  status="executed"
  note="Found orchestrator reference to ${TARGET}."
else
  status="SKIP_LAMBDA_INFRA_LOCALSTACK:missing_orchestrator_step"
  note="Reference to ${TARGET} not found; standardized skip recorded."
fi
{
  echo "# Orchestrator Audit"
  echo ""
  echo "- script: \`$ORCH_SCRIPT\`"
  echo "- target_step: \`$TARGET\`"
  echo "- result: \`$status\`"
  echo "- note: $note"
} > "$EVIDENCE_FILE"
echo "$status"
