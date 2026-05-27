variable "environment" {
  type        = string
  description = "Deployment environment token used in deterministic naming."
  default     = "local"
  validation {
    condition     = contains(["local", "dev", "stg", "prod"], var.environment)
    error_message = "environment must be one of local|dev|stg|prod."
  }
}
variable "owner" {
  type        = string
  description = "Tag owner/team identifier."
}
variable "cost_center" {
  type        = string
  description = "Tag cost center code."
}
variable "data_classification" {
  type        = string
  description = "Tag data classification enum."
  validation {
    condition     = contains(["public", "internal", "restricted"], var.data_classification)
    error_message = "data_classification must be public|internal|restricted."
  }
}
variable "aws_region" {
  type    = string
  default = "us-east-1"
}
variable "aws_access_key" {
  type    = string
  default = "test"
}
variable "aws_secret_key" {
  type    = string
  default = "test"
}
variable "localstack_endpoint" {
  type    = string
  default = "http://localhost:4566"
}

variable "source_queue_name" {
  type        = string
  description = "Existing SQS source queue name used by the Lambda event source mapping."
  default     = "car-status-queue"
}

variable "dlq_queue_name" {
  type        = string
  description = "Existing SQS dead-letter queue name used by the source queue redrive policy."
  default     = "car-status-dlq"
}
variable "car_api_base_url" {
  type = string
  validation {
    condition     = length(trimspace(var.car_api_base_url)) > 0
    error_message = "CAR_API_BASE_URL is required."
  }
}
variable "car_api_timeout_ms" {
  type = number
  validation {
    condition     = var.car_api_timeout_ms > 0
    error_message = "CAR_API_TIMEOUT_MS must be > 0."
  }
}
variable "keycloak_token_url" {
  type = string
  validation {
    condition     = length(trimspace(var.keycloak_token_url)) > 0
    error_message = "KEYCLOAK_TOKEN_URL is required."
  }
}
variable "keycloak_client_id" {
  type = string
  validation {
    condition     = length(trimspace(var.keycloak_client_id)) > 0
    error_message = "KEYCLOAK_CLIENT_ID is required."
  }
}
variable "keycloak_client_secret" {
  type        = string
  sensitive   = true
  description = "Raw Keycloak client secret value. Terraform persists it in Secrets Manager as JSON {\"client_secret\":\"...\"}."
  validation {
    condition     = length(trimspace(var.keycloak_client_secret)) > 0
    error_message = "KEYCLOAK_SECRET_ID backing secret value is required."
  }
}
variable "keycloak_refresh_skew_seconds" {
  type = number
  validation {
    condition     = var.keycloak_refresh_skew_seconds >= 0
    error_message = "KEYCLOAK_REFRESH_SKEW_SECONDS must be >= 0."
  }
}
variable "breaker_max_requests" {
  type = number
  validation {
    condition     = var.breaker_max_requests > 0
    error_message = "BREAKER_MAX_REQUESTS must be > 0."
  }
}
variable "breaker_interval_ms" {
  type = number
  validation {
    condition     = var.breaker_interval_ms > 0
    error_message = "BREAKER_INTERVAL_MS must be > 0."
  }
}
variable "breaker_timeout_ms" {
  type = number
  validation {
    condition     = var.breaker_timeout_ms > 0
    error_message = "BREAKER_TIMEOUT_MS must be > 0."
  }
}
variable "breaker_ready_to_trip_failures" {
  type = number
  validation {
    condition     = var.breaker_ready_to_trip_failures > 0
    error_message = "BREAKER_READY_TO_TRIP_FAILURES must be > 0."
  }
}
variable "log_level" {
  type = string
  validation {
    condition     = length(trimspace(var.log_level)) > 0
    error_message = "LOG_LEVEL is required."
  }
}
variable "new_relic_extension_layer_arn" {
  type    = string
  default = ""
}
variable "new_relic_account_id" {
  type = string
  validation {
    condition     = length(trimspace(var.new_relic_account_id)) > 0
    error_message = "NEW_RELIC_ACCOUNT_ID is required."
  }
}
variable "new_relic_trusted_account_key" {
  type      = string
  sensitive = true
  validation {
    condition     = length(trimspace(var.new_relic_trusted_account_key)) > 0
    error_message = "NEW_RELIC_TRUSTED_ACCOUNT_KEY is required."
  }
}
variable "new_relic_license_key_secret_arn" {
  type = string
  validation {
    condition     = length(trimspace(var.new_relic_license_key_secret_arn)) > 0
    error_message = "NEW_RELIC_LICENSE_KEY_SECRET_ARN is required."
  }
}
variable "queue_visibility_timeout_seconds" {
  type    = number
  default = 120
}
variable "queue_message_retention_seconds" {
  type    = number
  default = 1209600
}
variable "queue_max_receive_count" {
  type    = number
  default = 5
}
variable "log_retention_in_days" {
  type    = number
  default = 14
}
variable "lambda_memory_size" {
  type    = number
  default = 256
}
variable "lambda_timeout_seconds" {
  type    = number
  default = 30
}
variable "lambda_zip_path" {
  type    = string
  default = "../../lambda.zip"
}
