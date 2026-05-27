#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
RUNBOOK="${ROOT_DIR}/specs/002-lambda-infra-localstack/quickstart.md"
required=(
  "## 1) Prerequisites"
  "## 2) Build and validate runtime package contract"
  "## 5) Dependency-ordered infra deploy workflow"
  "Terraform has been successfully initialized!"
  "Success! The configuration is valid."
  "Apply complete! Resources:"
  "SMOKE_TEST_RESULT: PASS"
  "connection refused"
  "Recovery path"
)
for token in "${required[@]}"; do
  runbook_content="$(tr '[:upper:]' '[:lower:]' < "$RUNBOOK")"
  token_lc="$(printf '%s' "$token" | tr '[:upper:]' '[:lower:]')"
  [[ "$runbook_content" == *"$token_lc"* ]] || { echo "RUNBOOK_COMPLETENESS_RESULT: FAIL (missing: $token)"; exit 1; }
done
echo "RUNBOOK_COMPLETENESS_RESULT: PASS"
