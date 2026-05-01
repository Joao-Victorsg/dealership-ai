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

variable "jwt_issuer_uri" {
  description = "OAuth2 JWT issuer URI (spring.security.oauth2.resourceserver.jwt.issuer-uri)"
  type        = string
  default     = "http://keycloak:8080/realms/dealership"
}

variable "s3_bucket" {
  description = "S3 bucket name for car images"
  type        = string
  default     = "car-images"
}

variable "s3_region" {
  description = "AWS region for S3"
  type        = string
  default     = "us-east-1"
}

variable "s3_endpoint" {
  description = "S3 endpoint override — set to LocalStack URL for local development"
  type        = string
  default     = "http://s3.localhost.localstack.cloud:4566"
}

variable "s3_presigned_url_ttl" {
  description = "Presigned URL TTL in seconds"
  type        = number
  default     = 900
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
  default     = "car-api-dealership"
}
