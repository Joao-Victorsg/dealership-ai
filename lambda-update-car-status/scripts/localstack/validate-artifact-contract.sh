#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ARTIFACT="${ROOT_DIR}/lambda.zip"
[[ -f "$ARTIFACT" ]] || { echo "FAIL: lambda.zip not found"; exit 1; }
entries="$(unzip -Z1 "$ARTIFACT")"
if [[ "$entries" != "bootstrap" ]]; then
  echo "FAIL: lambda.zip must contain exactly bootstrap"
  exit 1
fi
tmp_dir="$(mktemp -d)"
unzip -q "$ARTIFACT" -d "$tmp_dir"
file "$tmp_dir/bootstrap" | grep -Eq 'ELF 64-bit.*ARM aarch64' || {
  echo "FAIL: bootstrap is not linux/arm64"
  rm -rf "$tmp_dir"
  exit 1
}
rm -rf "$tmp_dir"
echo "ARTIFACT_CONTRACT_RESULT: PASS"
