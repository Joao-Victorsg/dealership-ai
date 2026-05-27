resource "aws_secretsmanager_secret" "keycloak" {
  name = local.names.secret
  tags = local.required_tags
}
resource "aws_secretsmanager_secret_version" "keycloak" {
  secret_id = aws_secretsmanager_secret.keycloak.id
  secret_string = jsonencode({
    client_secret = var.keycloak_client_secret
  })
}
