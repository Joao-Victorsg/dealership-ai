resource "terraform_data" "required_runtime_config_contract" {
  input = {
    CAR_API_BASE_URL               = var.car_api_base_url
    CAR_API_TIMEOUT_MS             = var.car_api_timeout_ms
    KEYCLOAK_TOKEN_URL             = var.keycloak_token_url
    KEYCLOAK_CLIENT_ID             = var.keycloak_client_id
    KEYCLOAK_SECRET_ID             = aws_secretsmanager_secret.keycloak.id
    KEYCLOAK_REFRESH_SKEW_SECONDS  = var.keycloak_refresh_skew_seconds
    BREAKER_MAX_REQUESTS           = var.breaker_max_requests
    BREAKER_INTERVAL_MS            = var.breaker_interval_ms
    BREAKER_TIMEOUT_MS             = var.breaker_timeout_ms
    BREAKER_READY_TO_TRIP_FAILURES = var.breaker_ready_to_trip_failures
    LOG_LEVEL                      = var.log_level
  }
  lifecycle {
    precondition {
      condition     = length(trimspace(var.car_api_base_url)) > 0 && length(trimspace(var.keycloak_token_url)) > 0 && length(trimspace(var.keycloak_client_id)) > 0 && length(trimspace(var.log_level)) > 0
      error_message = "Required runtime config contract is incomplete."
    }
  }
}
