#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
INFRA_DIR="${ROOT_DIR}/infra/localstack"
VARS_FILE="${INFRA_DIR}/terraform.tfvars"
VARS_FILE_EXAMPLE="${INFRA_DIR}/terraform.tfvars.example"
for arg in "$@"; do
  if [[ "$arg" == *"tofu"* || "$arg" == *"opentofu"* ]]; then
    echo "FAIL: Terraform CLI is the only permitted IaC executor"
    exit 1
  fi
done
[[ "${IAC_EXECUTOR:-terraform}" == "terraform" ]] || { echo "FAIL: IAC_EXECUTOR must be terraform"; exit 1; }
"${ROOT_DIR}/scripts/localstack/preflight.sh"
"${ROOT_DIR}/scripts/localstack/validate-artifact-contract.sh"
"${ROOT_DIR}/scripts/localstack/check-deploy-orchestrator.sh"
terraform -chdir="$INFRA_DIR" fmt -check
terraform -chdir="$INFRA_DIR" init
terraform -chdir="$INFRA_DIR" validate
if [[ ! -f "$VARS_FILE" && -f "$VARS_FILE_EXAMPLE" ]]; then
  cp "$VARS_FILE_EXAMPLE" "$VARS_FILE"
  echo "INFO: Created terraform.tfvars from terraform.tfvars.example"
fi
if [[ -f "$VARS_FILE" ]]; then
  terraform -chdir="$INFRA_DIR" plan -var-file="$VARS_FILE" -out=tfplan
  terraform -chdir="$INFRA_DIR" apply -auto-approve tfplan
else
  terraform -chdir="$INFRA_DIR" plan -out=tfplan
  terraform -chdir="$INFRA_DIR" apply -auto-approve tfplan
fi
"${ROOT_DIR}/scripts/localstack/validate-inventory-conformance.sh"
"${ROOT_DIR}/scripts/localstack/verify-dependency-order.sh"
echo "DEPLOY_RESULT: PASS"
