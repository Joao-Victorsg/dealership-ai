resource "aws_lambda_function" "main" {
  function_name    = local.names.fn
  role             = aws_iam_role.lambda.arn
  runtime          = "provided.al2023"
  architectures    = ["arm64"]
  handler          = "bootstrap"
  filename         = var.lambda_zip_path
  source_code_hash = filebase64sha256(var.lambda_zip_path)
  timeout          = var.lambda_timeout_seconds
  memory_size      = var.lambda_memory_size
  environment {
    variables = local.runtime_env
  }
  layers = length(trimspace(var.new_relic_extension_layer_arn)) > 0 ? [var.new_relic_extension_layer_arn] : []
  depends_on = [
    aws_cloudwatch_log_group.lambda,
    aws_secretsmanager_secret_version.keycloak,
    aws_iam_role_policy.lambda
  ]
  tags = local.required_tags
}
