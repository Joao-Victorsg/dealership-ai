output "lambda_function_name" {
  value = aws_lambda_function.main.function_name
}
output "lambda_function_arn" {
  value = aws_lambda_function.main.arn
}
output "event_source_mapping_uuid" {
  value = aws_lambda_event_source_mapping.source.uuid
}
