resource "aws_cloudwatch_log_group" "lambda" {
  name              = "/aws/lambda/${local.names.fn}"
  retention_in_days = var.log_retention_in_days
  tags              = local.required_tags
}
