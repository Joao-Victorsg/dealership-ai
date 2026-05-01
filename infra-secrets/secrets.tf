resource "aws_secretsmanager_secret" "database_password" {
  name                    = "database-password"
  description             = "Master password for the car-api Aurora PostgreSQL cluster"
  recovery_window_in_days = 0

  tags = {
    Name    = "database-password"
    Project = "dealership-ai"
  }
}

resource "aws_secretsmanager_secret_version" "database_password" {
  secret_id     = aws_secretsmanager_secret.database_password.id
  secret_string = "changeme"
}

resource "aws_secretsmanager_secret" "client_database_password" {
  name                    = "client-api-database-password"
  description             = "Master password for the client-api Aurora PostgreSQL cluster"
  recovery_window_in_days = 0

  tags = {
    Name    = "client-api-database-password"
    Project = "dealership-ai"
  }
}

resource "aws_secretsmanager_secret_version" "client_database_password" {
  secret_id     = aws_secretsmanager_secret.client_database_password.id
  secret_string = "changeme"
}

resource "aws_secretsmanager_secret" "sales_database_password" {
  name                    = "sales-api-database-password"
  description             = "Master password for the sales-api Aurora PostgreSQL cluster"
  recovery_window_in_days = 0

  tags = {
    Name    = "sales-api-database-password"
    Project = "dealership-ai"
  }
}

resource "aws_secretsmanager_secret_version" "sales_database_password" {
  secret_id     = aws_secretsmanager_secret.sales_database_password.id
  secret_string = "changeme"
}
