data "aws_secretsmanager_secret" "database_password" {
  name = "database-password"
}

data "aws_secretsmanager_secret" "client_database_password" {
  name = "client-api-database-password"
}

data "aws_secretsmanager_secret" "sales_database_password" {
  name = "sales-api-database-password"
}
