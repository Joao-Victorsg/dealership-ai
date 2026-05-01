resource "aws_ssm_parameter" "database_username" {
  name        = "/dealership-ai/api/database/username"
  description = "Master username for the car-api Aurora PostgreSQL cluster"
  type        = "SecureString"
  value       = var.database_username

  tags = {
    Name    = "dealership-ai-db-username"
    Project = "dealership-ai"
  }
}

# Stores the Secrets Manager ARN — infra-databases reads this to retrieve the actual password.
resource "aws_ssm_parameter" "database_password" {
  name        = "/dealership-ai/api/database/password"
  description = "ARN of the Secrets Manager secret containing the database password"
  type        = "String"
  value       = data.aws_secretsmanager_secret.database_password.arn

  tags = {
    Name    = "dealership-ai-db-password-arn"
    Project = "dealership-ai"
  }
}

resource "aws_ssm_parameter" "s3_bucket" {
  name        = "/dealership-ai/api/s3/bucket"
  description = "S3 bucket name for car images"
  type        = "String"
  value       = "car-images"

  tags = {
    Name    = "dealership-ai-s3-bucket"
    Project = "dealership-ai"
  }
}

resource "aws_ssm_parameter" "s3_region" {
  name        = "/dealership-ai/api/s3/region"
  description = "AWS region for S3 operations"
  type        = "String"
  value       = "us-east-1"

  tags = {
    Name    = "dealership-ai-s3-region"
    Project = "dealership-ai"
  }
}

resource "aws_ssm_parameter" "jwt_issuer_uri" {
  name        = "/dealership-ai/api/jwt/issuer-uri"
  description = "OAuth2 JWT issuer URI for Spring Security resource server"
  type        = "String"
  value       = var.jwt_issuer_uri

  tags = {
    Name    = "dealership-ai-jwt-issuer"
    Project = "dealership-ai"
  }
}

# ---------------------------------------------------------------------------
# client-api database credentials
# ---------------------------------------------------------------------------

resource "aws_ssm_parameter" "client_database_username" {
  name        = "/dealership-ai/client-api/database/username"
  description = "Master username for the client-api Aurora PostgreSQL cluster"
  type        = "SecureString"
  value       = var.client_database_username

  tags = {
    Name    = "dealership-ai-client-db-username"
    Project = "dealership-ai"
  }
}

resource "aws_ssm_parameter" "client_database_password" {
  name        = "/dealership-ai/client-api/database/password"
  description = "ARN of the Secrets Manager secret containing the client-api database password"
  type        = "String"
  value       = data.aws_secretsmanager_secret.client_database_password.arn

  tags = {
    Name    = "dealership-ai-client-db-password-arn"
    Project = "dealership-ai"
  }
}

# ---------------------------------------------------------------------------
# sales-api database credentials
# ---------------------------------------------------------------------------

resource "aws_ssm_parameter" "sales_database_username" {
  name        = "/dealership-ai/sales-api/database/username"
  description = "Master username for the sales-api Aurora PostgreSQL cluster"
  type        = "SecureString"
  value       = var.sales_database_username

  tags = {
    Name    = "dealership-ai-sales-db-username"
    Project = "dealership-ai"
  }
}

resource "aws_ssm_parameter" "sales_database_password" {
  name        = "/dealership-ai/sales-api/database/password"
  description = "ARN of the Secrets Manager secret containing the sales-api database password"
  type        = "String"
  value       = data.aws_secretsmanager_secret.sales_database_password.arn

  tags = {
    Name    = "dealership-ai-sales-db-password-arn"
    Project = "dealership-ai"
  }
}
