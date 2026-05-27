variable "image_tag" {
  description = "Docker image tag to deploy (e.g. '1.0.0' or 'latest')"
  type        = string
  default     = "latest"
}

variable "bff_url" {
  description = "Server-side BFF base URL used by Next.js server code"
  type        = string
  default     = "http://host.docker.internal:8083"
}

variable "next_public_bff_url" {
  description = "Browser-facing BFF URL used by frontend client code and redirects"
  type        = string
  default     = "https://app.localhost:4443"
}

variable "next_public_app_url" {
  description = "Browser-facing frontend app URL"
  type        = string
  default     = "https://app.localhost:4443"
}

variable "next_public_keycloak_url" {
  description = "Browser-facing Keycloak URL used by CSP/connect-src and auth flows"
  type        = string
  default     = "https://auth.localhost:4443"
}
