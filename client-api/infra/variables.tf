variable "image_tag" {
  description = "Docker image tag to deploy (e.g. '1.0.0' or 'latest')"
  type        = string
  default     = "latest"
}

variable "db_host" {
  description = "Aurora PostgreSQL cluster writer endpoint"
  type        = string
  default     = "localhost.localstack.cloud"
}

variable "db_port" {
  description = "Aurora PostgreSQL port"
  type        = number
  default     = 4510
}

variable "db_name" {
  description = "Database name"
  type        = string
  default     = "dealershipdb"
}

variable "db_username" {
  description = "Database master username"
  type        = string
  default     = "dealership"
}

variable "db_password" {
  description = "Database master password"
  type        = string
  sensitive   = true
  default     = "changeme"
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

variable "jwks_uri" {
  description = "OAuth2 JWK Set URI (spring.security.oauth2.resourceserver.jwt.jwk-set-uri)"
  type        = string
  default     = "https://idp.example.com/realms/dealership/protocol/openid-connect/certs"
}

variable "cpf_encryption_key" {
  description = "AES-256-GCM key for CPF encryption at rest (Base64-encoded, 32 bytes)"
  type        = string
  sensitive   = true
  default     = ""
}

variable "cpf_hmac_secret" {
  description = "HMAC-SHA256 secret for CPF uniqueness hash (Base64-encoded)"
  type        = string
  sensitive   = true
  default     = ""
}

variable "viacep_base_url" {
  description = "ViaCEP base URL for address lookup"
  type        = string
  default     = "https://viacep.com.br"
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
  default     = "client-api-dealership"
}
