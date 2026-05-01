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
  default     = 4513
}

variable "db_name" {
  description = "Database name"
  type        = string
  default     = "salesdb"
}

variable "db_username" {
  description = "Database master username"
  type        = string
  default     = "sales"
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
  default     = "http://keycloak:8080/realms/dealership/protocol/openid-connect/certs"
}

variable "sns_topic_arn" {
  description = "ARN of the SNS topic for sale events"
  type        = string
  default     = "arn:aws:sns:us-east-1:000000000000:sale-events"
}

variable "aws_region" {
  description = "AWS region used by the application's SNS client"
  type        = string
  default     = "us-east-1"
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
  default     = "sales-api-dealership"
}
