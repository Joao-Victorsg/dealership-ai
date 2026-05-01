variable "image_tag" {
  description = "Docker image tag to deploy (e.g. '1.0.0' or 'latest')"
  type        = string
  default     = "latest"
}

variable "redis_host" {
  description = "ElastiCache Redis cluster endpoint"
  type        = string
  default     = "localhost.localstack.cloud"
}

variable "redis_port" {
  description = "ElastiCache Redis port"
  type        = number
  default     = 4511
}

variable "keycloak_base_url" {
  description = "Base URL of the Keycloak server (e.g. http://keycloak:8080)"
  type        = string
  default     = "http://keycloak:8080"
}

variable "car_api_base_url" {
  description = "Base URL for the car-api service (e.g. http://<nlb-dns>:8080)"
  type        = string
  default     = "http://host.docker.internal:8080"
}

variable "client_api_base_url" {
  description = "Base URL for the client-api service (e.g. http://<nlb-dns>:8081)"
  type        = string
  default     = "http://host.docker.internal:8081"
}

variable "sales_api_base_url" {
  description = "Base URL for the sales-api service (e.g. http://<nlb-dns>:8082)"
  type        = string
  default     = "http://host.docker.internal:8082"
}

variable "keycloak_realm" {
  description = "Keycloak realm name"
  type        = string
  default     = "dealership"
}

variable "keycloak_client_id" {
  description = "Keycloak client ID used by the BFF"
  type        = string
  default     = "dealership-bff"
}

variable "keycloak_client_secret" {
  description = "Keycloak client secret for the BFF OAuth2 client (both login and registration flows)"
  type        = string
  sensitive   = true
}

variable "app_post_login_redirect_uri" {
  description = "Frontend URL to redirect to after successful login (e.g. https://app.example.com)"
  type        = string
  default     = "http://localhost:3000"
}

variable "app_post_logout_redirect_uri" {
  description = "Frontend URL to redirect to after logout (e.g. https://app.example.com)"
  type        = string
  default     = "http://localhost:3000"
}

variable "app_post_registration_redirect_uri" {
  description = "Frontend URL to redirect to after Keycloak registration (e.g. https://app.example.com/register/complete)"
  type        = string
  default     = "http://localhost:3000/register/complete"
}

variable "new_relic_license_key" {
  description = "New Relic license key passed to the Java agent via NEW_RELIC_LICENSE_KEY env var"
  type        = string
  sensitive   = true
  default     = ""
}

variable "new_relic_app_name" {
  description = "Application name shown in New Relic UI"
  type        = string
  default     = "dealership-bff"
}
