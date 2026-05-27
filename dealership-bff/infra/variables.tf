variable "image_tag" {
  description = "Docker image tag to deploy (e.g. '1.0.0' or 'latest')"
  type        = string
  default     = "latest"
}

variable "redis_host" {
  description = "ElastiCache Redis cluster endpoint override (optional)"
  type        = string
  default     = null
  nullable    = true
}

variable "redis_port" {
  description = "ElastiCache Redis port override (optional)"
  type        = number
  default     = null
  nullable    = true
}

variable "keycloak_base_url" {
  description = "Internal Keycloak URL for server-to-server calls (token exchange, JWK, userinfo)"
  type        = string
  default     = "http://keycloak:8080"
}

variable "keycloak_external_url" {
  description = "Browser-facing Keycloak URL embedded in the OAuth2 authorization redirect (must be resolvable by the end-user's browser)"
  type        = string
  default     = "https://auth.localhost:4443"
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
  default     = "dealership-bff-secret"
}

variable "keycloak_system_client_id" {
  description = "Keycloak system client ID used for machine-to-machine calls from BFF to downstream APIs"
  type        = string
  default     = "dealership-system"
}

variable "keycloak_system_client_secret" {
  description = "Keycloak system client secret used for machine-to-machine calls from BFF to downstream APIs"
  type        = string
  sensitive   = true
  default     = "dealership-system-secret"
}

variable "app_post_login_redirect_uri" {
  description = "Frontend URL to redirect to after successful login (e.g. https://app.example.com)"
  type        = string
  default     = "https://app.localhost:4443"
}

variable "app_post_logout_redirect_uri" {
  description = "Frontend URL to redirect to after logout (e.g. https://app.example.com)"
  type        = string
  default     = "https://app.localhost:4443/"
}

variable "app_post_registration_redirect_uri" {
  description = "Frontend URL to redirect to after Keycloak registration (e.g. https://app.example.com/complete-registration)"
  type        = string
  default     = "https://app.localhost:4443/complete-registration"
}

variable "session_cookie_secure" {
  description = "Whether to mark BFF session cookie as Secure"
  type        = bool
  default     = true
}

variable "session_cookie_same_site" {
  description = "SameSite policy for BFF session cookie (Strict, Lax, or None)"
  type        = string
  default     = "lax"

  validation {
    condition     = contains(["strict", "lax", "none"], lower(var.session_cookie_same_site))
    error_message = "session_cookie_same_site must be one of: strict, lax, none."
  }
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
