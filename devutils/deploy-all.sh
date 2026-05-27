#!/bin/bash

# ---------------------------------------------------------------------------
# deploy-all.sh — Deploys the full dealership-ai stack to LocalStack
# Run from: d:/JV/Projetos/dealership-ai/devutils/
# ---------------------------------------------------------------------------

BASE_DIR=$(pwd)
ERRORS=0
declare -a ERROR_MESSAGES

ECR_REGISTRY="000000000000.dkr.ecr.us-east-1.localhost.localstack.cloud:4566"
IMAGE_TAG="latest"
KEYCLOAK_THEME_BUILD_SCRIPT="$BASE_DIR/keycloak/build-keycloakify-theme.sh"

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

section() { echo ""; echo "=========================================="; echo "  $1"; echo "=========================================="; }
ok()      { echo "✅ $1"; }
fail()    { echo "❌ $1"; }
info()    { echo "ℹ️  $1"; }

if [ -n "${NEW_RELIC_LICENSE_KEY:-}" ]; then
    export TF_VAR_new_relic_license_key="${NEW_RELIC_LICENSE_KEY}"
    info "New Relic license key detected in environment; backend modules will receive it via TF_VAR_new_relic_license_key."
else
    unset TF_VAR_new_relic_license_key
    info "NEW_RELIC_LICENSE_KEY not set; backend modules will use their default New Relic license key value."
fi

# ---------------------------------------------------------------------------
# 0. Build Keycloakify login theme
# ---------------------------------------------------------------------------
section "Keycloakify theme"
info "Building and exporting Keycloakify login theme..."
if bash "$KEYCLOAK_THEME_BUILD_SCRIPT"; then
    ok "Keycloakify theme exported"
else
    ERROR_MESSAGES+=("❌ Keycloakify theme build/export failed — run ./keycloak/build-keycloakify-theme.sh manually")
    ((ERRORS++))
fi

# ---------------------------------------------------------------------------
# 1. Remove stale LocalStack volume and restart
# ---------------------------------------------------------------------------
section "LocalStack"

read -p "Restart LocalStack from scratch? This removes the volume (s/n): " RESTART_LS
if [ "$RESTART_LS" == "s" ]; then
    info "Stopping containers and removing LocalStack volume..."
    docker compose down 2>/dev/null || true
    rm -rf "$BASE_DIR/volume"
    ok "Volume removed"
fi

info "Starting Docker Compose services..."
docker compose up -d --build
ok "Docker Compose started"

echo ""
info "Waiting for LocalStack to initialize (5 seconds)..."
sleep 5
ok "LocalStack initialized"

# ---------------------------------------------------------------------------
# Keycloak setup — configure realm, roles, clients, and users.
# The script itself waits for Keycloak to become healthy before proceeding.
# ---------------------------------------------------------------------------
section "Keycloak"
info "Waiting for Keycloak to become healthy..."
KC_URL="http://localhost:8180/realms/master"
KC_RETRIES=30
for i in $(seq 1 $KC_RETRIES); do
    if curl -fsS "$KC_URL" -o /dev/null 2>/dev/null; then
        ok "Keycloak is healthy"
        break
    fi
    if [ "$i" -eq "$KC_RETRIES" ]; then
        ERROR_MESSAGES+=("❌ Keycloak did not become healthy after ${KC_RETRIES}s — run ./keycloak/setup-keycloak.sh manually")
        ((ERRORS++))
    fi
    sleep 2
done

info "Running Keycloak setup script..."
if bash "$BASE_DIR/keycloak/setup-keycloak.sh"; then
    ok "Keycloak setup completed"
else
    ERROR_MESSAGES+=("❌ Keycloak setup failed — run ./keycloak/setup-keycloak.sh manually after Keycloak is healthy")
    ((ERRORS++))
fi

# ---------------------------------------------------------------------------
# 2. Ask whether to destroy+reinitialise Terraform state
# ---------------------------------------------------------------------------
section "Terraform"

read -p "Destroy existing Terraform state before applying? (s/n): " DESTROY_TF

manage_terraform() {
    local rel_dir="$1"
    local description="$2"
    local abs_dir="$BASE_DIR/$rel_dir"
    local tfvars_file=""
    local -a apply_args

    section "Terraform: $description"
    cd "$abs_dir" || { ERROR_MESSAGES+=("❌ Directory not found: $rel_dir"); ((ERRORS++)); return 1; }

    if [ "$DESTROY_TF" == "s" ]; then
        info "Removing Terraform state files in $rel_dir..."
        rm -rf .terraform .terraform.lock.hcl terraform.tfstate terraform.tfstate.backup
        ok "State files removed"
    fi

    info "terraform init..."
    if ! terraform init -input=false -upgrade 2>&1; then
        ERROR_MESSAGES+=("❌ terraform init failed in: $rel_dir")
        ((ERRORS++))
        cd "$BASE_DIR"
        return 1
    fi

    if [ -f "terraform.tfvars" ]; then
        tfvars_file="terraform.tfvars"
    elif [ -f "terraform.tfvars.example" ]; then
        info "terraform.tfvars not found in $rel_dir; creating it from terraform.tfvars.example"
        cp "terraform.tfvars.example" "terraform.tfvars"
        tfvars_file="terraform.tfvars"
    fi

    apply_args=(-auto-approve -input=false)
    if [ -n "$tfvars_file" ]; then
        info "Using Terraform variables file: $tfvars_file"
        apply_args+=("-var-file=$tfvars_file")
    fi

    info "terraform apply..."
    if ! terraform apply "${apply_args[@]}" 2>&1; then
        ERROR_MESSAGES+=("❌ terraform apply failed in: $rel_dir")
        ((ERRORS++))
        cd "$BASE_DIR"
        return 1
    fi

    ok "$description applied successfully"
    cd "$BASE_DIR"
    return 0
}

manage_optional_terraform() {
    local rel_dir="$1"
    local description="$2"
    local abs_dir="$BASE_DIR/$rel_dir"

    if [ ! -d "$abs_dir" ]; then
        info "Skipping $description (directory not found: $rel_dir)"
        return 0
    fi

    manage_terraform "$rel_dir" "$description"
}

# ---------------------------------------------------------------------------
# 3. Deploy ECR first (needed before image push)
# ---------------------------------------------------------------------------
manage_terraform "../infra-ecr" "ECR repositories"

# ---------------------------------------------------------------------------
# 4. Build and push all service images to LocalStack ECR
# ---------------------------------------------------------------------------

build_and_push() {
    local service_dir="$1"   # relative path from BASE_DIR, e.g. ../car-api
    local image_name="$2"    # e.g. joaovictorsg/car-api-dealership

    local service_label
    service_label=$(basename "$service_dir")
    section "Docker — build & push $service_label"

    local local_tag="$image_name:$IMAGE_TAG"
    local remote_tag="$ECR_REGISTRY/$image_name:$IMAGE_TAG"

    info "Building image: $local_tag"
    cd "$BASE_DIR/$service_dir" || { fail "$service_label directory not found"; ERROR_MESSAGES+=("❌ Directory not found: $service_dir"); ((ERRORS++)); cd "$BASE_DIR"; return 1; }

    if ! docker build -t "$local_tag" .; then
        fail "Docker build failed for $service_label"
        ERROR_MESSAGES+=("❌ Docker build failed for $service_label")
        ((ERRORS++))
        cd "$BASE_DIR"
        return 1
    fi
    ok "Image built: $local_tag"

    info "Tagging as $remote_tag"
    docker tag "$local_tag" "$remote_tag"

    info "Pushing to LocalStack ECR..."
    if ! docker push "$remote_tag"; then
        fail "Docker push failed for $service_label"
        ERROR_MESSAGES+=("❌ Docker push failed for $service_label")
        ((ERRORS++))
        cd "$BASE_DIR"
        return 1
    fi
    ok "Image pushed: $remote_tag"

    cd "$BASE_DIR"
}

build_and_push "../car-api"        "joaovictorsg/car-api-dealership"
build_and_push "../client-api"     "joaovictorsg/client-api-dealership"
build_and_push "../sales-api"      "joaovictorsg/sales-api-dealership"
build_and_push "../dealership-bff" "joaovictorsg/dealership-bff"
build_and_push "../dealership-web" "joaovictorsg/dealership-web"

# ---------------------------------------------------------------------------
# 5. Apply remaining infra modules in dependency order
# ---------------------------------------------------------------------------
BACKEND_INFRA_DIRS=(
    "../infra-secrets"
    "../infra-vpc"
    "../infra-sns"
    "../infra-sqs"
    "../infra-parameters"
    "../infra-s3"
    "../infra-ses"
    "../infra-databases"
    "../infra-elasticache"
    "../car-api/infra"
    "../client-api/infra"
    "../sales-api/infra"
    "../lambda-invoice-processor/infra"
    "../lambda-send-email/infra"
    "../infra-step-function"
    "../lambda-start-invoice-workflow/infra"
)

for dir in "${BACKEND_INFRA_DIRS[@]}"; do
    manage_terraform "$dir" "$(basename "$dir")" || info "Continuing despite error in $dir..."
done

# Deploy lambda-update-car-status LocalStack infrastructure.
# This module is kept optional while infra files are being introduced.
manage_optional_terraform "../lambda-update-car-status/infra/localstack" "lambda-update-car-status/infra/localstack" || info "Continuing despite error in ../lambda-update-car-status/infra/localstack..."

# Deploy the BFF — NLB listener ports now match container ports (8080/8081/8082),
# so the default values in variables.tf are always correct.
manage_terraform "../dealership-bff/infra" "dealership-bff/infra"
manage_terraform "../dealership-web/infra" "dealership-web/infra"

# ---------------------------------------------------------------------------
# 6. Seed sample data (cars, client profile, and admin/client users)
# ---------------------------------------------------------------------------
section "Seed sample data"
info "Running seed script..."
if bash "$BASE_DIR/seed/seed-sample-data.sh"; then
    ok "Seed data completed"
else
    ERROR_MESSAGES+=("❌ sample data seed failed in: ./seed/seed-sample-data.sh")
    ((ERRORS++))
fi

# ---------------------------------------------------------------------------
# 7. Final report
# ---------------------------------------------------------------------------
section "Summary"

NLB="api-dealership-ai.elb.localhost.localstack.cloud"

if [ $ERRORS -eq 0 ]; then
    echo ""
    echo "🎉 All deployments completed successfully!"
    echo ""
    echo "car-api"
    echo "  Health : http://$NLB:8080/actuator/health"
    echo "  Swagger: http://$NLB:8080/swagger-ui.html"
    echo ""
    echo "client-api"
    echo "  Health : http://$NLB:8081/actuator/health"
    echo "  Swagger: http://$NLB:8081/swagger-ui.html"
    echo ""
    echo "sales-api"
    echo "  Health : http://$NLB:8082/actuator/health"
    echo "  Swagger: http://$NLB:8082/swagger-ui.html"
    echo ""
    echo "dealership-bff  (NLB — use API GW invoke URL for frontend)"
    echo "  Health : http://$NLB:8083/actuator/health"
    echo "  Swagger: http://$NLB:8083/swagger-ui.html"
    echo ""
    echo "dealership-web"
    echo "  URL    : http://$NLB:3000"
    echo ""
else
    echo ""
    echo "🚨 Completed with $ERRORS error(s):"
    for msg in "${ERROR_MESSAGES[@]}"; do echo "  $msg"; done
    echo ""
fi

exit $ERRORS
