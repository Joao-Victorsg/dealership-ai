data "aws_vpc" "vpc" {
  filter {
    name   = "tag:Name"
    values = ["vpc_dealership_ai"]
  }
}

data "aws_subnets" "private" {
  filter {
    name   = "tag:Name"
    values = ["aws_subnet_private"]
  }

  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.vpc.id]
  }
}

# ---------------------------------------------------------------------------
# car-api credentials
# ---------------------------------------------------------------------------

data "aws_ssm_parameter" "database_username" {
  name            = "/dealership-ai/api/database/username"
  with_decryption = true
}

data "aws_ssm_parameter" "database_password_arn" {
  name = "/dealership-ai/api/database/password"
}

data "aws_secretsmanager_secret" "database_password" {
  arn = data.aws_ssm_parameter.database_password_arn.value
}

data "aws_secretsmanager_secret_version" "database_password" {
  secret_id = data.aws_secretsmanager_secret.database_password.id
}

# ---------------------------------------------------------------------------
# client-api credentials
# ---------------------------------------------------------------------------

data "aws_ssm_parameter" "client_database_username" {
  name            = "/dealership-ai/client-api/database/username"
  with_decryption = true
}

data "aws_ssm_parameter" "client_database_password_arn" {
  name = "/dealership-ai/client-api/database/password"
}

data "aws_secretsmanager_secret" "client_database_password" {
  arn = data.aws_ssm_parameter.client_database_password_arn.value
}

data "aws_secretsmanager_secret_version" "client_database_password" {
  secret_id = data.aws_secretsmanager_secret.client_database_password.id
}

# ---------------------------------------------------------------------------
# sales-api credentials
# ---------------------------------------------------------------------------

data "aws_ssm_parameter" "sales_database_username" {
  name            = "/dealership-ai/sales-api/database/username"
  with_decryption = true
}

data "aws_ssm_parameter" "sales_database_password_arn" {
  name = "/dealership-ai/sales-api/database/password"
}

data "aws_secretsmanager_secret" "sales_database_password" {
  arn = data.aws_ssm_parameter.sales_database_password_arn.value
}

data "aws_secretsmanager_secret_version" "sales_database_password" {
  secret_id = data.aws_secretsmanager_secret.sales_database_password.id
}
