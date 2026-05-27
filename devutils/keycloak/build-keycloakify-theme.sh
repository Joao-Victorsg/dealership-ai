#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEVUTILS_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
THEME_PROJECT_DIR="${DEVUTILS_DIR}/keycloakify-theme"
THEME_OUTPUT_DIR="${SCRIPT_DIR}/themes"
THEME_NAME="aurelio-keycloakify"
THEME_JAR="${THEME_PROJECT_DIR}/dist_keycloak/keycloak-theme-for-kc-all-other-versions.jar"
TMP_DIR="$(mktemp -d)"

cleanup() {
  rm -rf "${TMP_DIR}"
}
trap cleanup EXIT

echo "[keycloak-theme] Installing dependencies..."
cd "${THEME_PROJECT_DIR}"
npm install --no-audit --no-fund

echo "[keycloak-theme] Building Keycloakify theme..."
npm run build-keycloak-theme

if [[ ! -f "${THEME_JAR}" ]]; then
  echo "[keycloak-theme] ERROR: Theme artifact not found at ${THEME_JAR}" >&2
  exit 1
fi

echo "[keycloak-theme] Extracting theme artifact..."
(
  cd "${TMP_DIR}"
  jar xf "${THEME_JAR}"
)

if [[ ! -d "${TMP_DIR}/theme/${THEME_NAME}" ]]; then
  echo "[keycloak-theme] ERROR: Theme '${THEME_NAME}' not found in artifact." >&2
  exit 1
fi

echo "[keycloak-theme] Publishing theme to ${THEME_OUTPUT_DIR}/${THEME_NAME}..."
rm -rf "${THEME_OUTPUT_DIR}/${THEME_NAME}"
mkdir -p "${THEME_OUTPUT_DIR}"
cp -R "${TMP_DIR}/theme/${THEME_NAME}" "${THEME_OUTPUT_DIR}/${THEME_NAME}"

echo "[keycloak-theme] Theme exported successfully."
