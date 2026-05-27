locals {
  feature_service      = "lambda-update-car-status"
  naming_pattern_envs  = ["local", "dev", "stg", "prod"]
  naming_pattern_types = ["fn", "queue", "dlq", "log", "role", "policy", "secret", "mapping"]
  names = {
    fn      = format("%s-%s-fn-01", var.environment, local.feature_service)
    queue   = format("%s-%s-queue-01", var.environment, local.feature_service)
    dlq     = format("%s-%s-dlq-01", var.environment, local.feature_service)
    log     = format("%s-%s-log-01", var.environment, local.feature_service)
    role    = format("%s-%s-role-01", var.environment, local.feature_service)
    policy  = format("%s-%s-policy-01", var.environment, local.feature_service)
    secret  = format("%s-%s-secret-01", var.environment, local.feature_service)
    mapping = format("%s-%s-mapping-01", var.environment, local.feature_service)
  }
  required_tags = {
    service             = local.feature_service
    environment         = var.environment
    managed_by          = "terraform"
    owner               = var.owner
    cost_center         = var.cost_center
    data_classification = var.data_classification
  }
  runtime_env = {
    CAR_API_BASE_URL                 = var.car_api_base_url
    CAR_API_TIMEOUT_MS               = tostring(var.car_api_timeout_ms)
    KEYCLOAK_TOKEN_URL               = var.keycloak_token_url
    KEYCLOAK_CLIENT_ID               = var.keycloak_client_id
    KEYCLOAK_SECRET_ID               = aws_secretsmanager_secret.keycloak.id
    KEYCLOAK_REFRESH_SKEW_SECONDS    = tostring(var.keycloak_refresh_skew_seconds)
    BREAKER_MAX_REQUESTS             = tostring(var.breaker_max_requests)
    BREAKER_INTERVAL_MS              = tostring(var.breaker_interval_ms)
    BREAKER_TIMEOUT_MS               = tostring(var.breaker_timeout_ms)
    BREAKER_READY_TO_TRIP_FAILURES   = tostring(var.breaker_ready_to_trip_failures)
    LOG_LEVEL                        = var.log_level
    NEW_RELIC_LAMBDA_HANDLER         = "bootstrap"
    NEW_RELIC_EXTENSION_LAYER_ARN    = var.new_relic_extension_layer_arn
    NEW_RELIC_ACCOUNT_ID             = var.new_relic_account_id
    NEW_RELIC_TRUSTED_ACCOUNT_KEY    = var.new_relic_trusted_account_key
    NEW_RELIC_LICENSE_KEY_SECRET_ARN = var.new_relic_license_key_secret_arn
  }
}
