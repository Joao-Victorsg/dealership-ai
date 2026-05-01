variable "database_username" {
  description = "Master username for the car-api Aurora PostgreSQL cluster"
  type        = string
  default     = "dealership"
}

variable "client_database_username" {
  description = "Master username for the client-api Aurora PostgreSQL cluster"
  type        = string
  default     = "dealership"
}

variable "sales_database_username" {
  description = "Master username for the sales-api Aurora PostgreSQL cluster"
  type        = string
  default     = "sales"
}

variable "jwt_issuer_uri" {
  description = "OAuth2 JWT issuer URI for Spring Security resource server"
  type        = string
  default     = "https://idp.example.com/realms/dealership"
}
