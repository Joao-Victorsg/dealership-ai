resource "aws_lambda_event_source_mapping" "source" {
  event_source_arn = data.aws_sqs_queue.source.arn
  function_name    = aws_lambda_function.main.arn
  enabled          = true
  batch_size       = 10
  depends_on = [
    aws_secretsmanager_secret_version.keycloak,
    aws_lambda_function.main
  ]
  tags = local.required_tags
}
