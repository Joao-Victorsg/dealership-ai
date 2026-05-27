#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
INFRA_DIR="${ROOT_DIR}/infra/localstack"
command -v terraform >/dev/null 2>&1 || { echo "terraform missing"; exit 1; }
command -v jq >/dev/null 2>&1 || { echo "jq missing"; exit 1; }
json="$(terraform -chdir="$INFRA_DIR" show -json)"
name_regex='^(local|dev|stg|prod)-lambda-update-car-status-(fn|queue|dlq|log|role|policy|secret|mapping)-[0-9]{2}$'
violations=0
total=0
while IFS= read -r encoded; do
  total=$((total+1))
  name="$(echo "$encoded" | jq -r '.values.name // empty')"
  tags="$(echo "$encoded" | jq -c '.values.tags // {}')"
  [[ -n "$name" && "$name" =~ $name_regex ]] || { echo "name violation: $name"; violations=$((violations+1)); }
  for k in service environment managed_by owner cost_center data_classification; do
    echo "$tags" | jq -e --arg k "$k" 'has($k)' >/dev/null || { echo "tag key missing: $k on $name"; violations=$((violations+1)); }
  done
  [[ "$(echo "$tags" | jq -r '.service // empty')" == "lambda-update-car-status" ]] || { echo "tag value violation: service on $name"; violations=$((violations+1)); }
  [[ "$(echo "$tags" | jq -r '.managed_by // empty')" == "terraform" ]] || { echo "tag value violation: managed_by on $name"; violations=$((violations+1)); }
  dc="$(echo "$tags" | jq -r '.data_classification // empty')"
  [[ "$dc" == "public" || "$dc" == "internal" || "$dc" == "restricted" ]] || { echo "tag value violation: data_classification on $name"; violations=$((violations+1)); }
done < <(echo "$json" | jq -c '.values.root_module.resources[] | select(.type|startswith("aws_"))')
[[ "$total" -gt 0 ]] || { echo "FAIL: no managed resources found in state"; exit 1; }
[[ "$violations" -eq 0 ]] || { echo "INVENTORY_CONFORMANCE_RESULT: FAIL ($violations violations)"; exit 1; }
echo "INVENTORY_CONFORMANCE_RESULT: PASS (resources=$total)"
