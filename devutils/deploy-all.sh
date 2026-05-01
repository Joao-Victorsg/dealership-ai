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

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

section() { echo ""; echo "=========================================="; echo "  $1"; echo "=========================================="; }
ok()      { echo "✅ $1"; }
fail()    { echo "❌ $1"; }
info()    { echo "ℹ️  $1"; }

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
docker compose up -d
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

    info "terraform apply..."
    if ! terraform apply -auto-approve -input=false 2>&1; then
        ERROR_MESSAGES+=("❌ terraform apply failed in: $rel_dir")
        ((ERRORS++))
        cd "$BASE_DIR"
        return 1
    fi

    ok "$description applied successfully"
    cd "$BASE_DIR"
    return 0
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

# ---------------------------------------------------------------------------
# 5. Apply remaining infra modules in dependency order
# ---------------------------------------------------------------------------
BACKEND_INFRA_DIRS=(
    "../infra-secrets"
    "../infra-vpc"
    "../infra-parameters"
    "../infra-s3"
    "../infra-databases"
    "../infra-elasticache"
    "../car-api/infra"
    "../client-api/infra"
    "../sales-api/infra"
)

for dir in "${BACKEND_INFRA_DIRS[@]}"; do
    manage_terraform "$dir" "$(basename "$dir")" || info "Continuing despite error in $dir..."
done

# Deploy the BFF — NLB listener ports now match container ports (8080/8081/8082),
# so the default values in variables.tf are always correct.
manage_terraform "../dealership-bff/infra" "dealership-bff/infra"

# ---------------------------------------------------------------------------
# 6. Final report
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
else
    echo ""
    echo "🚨 Completed with $ERRORS error(s):"
    for msg in "${ERROR_MESSAGES[@]}"; do echo "  $msg"; done
    echo ""
fi

exit $ERRORS
