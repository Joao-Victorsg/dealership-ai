resource "aws_lambda_event_source_mapping" "invoice_queue" {
  event_source_arn = data.aws_sqs_queue.invoice_queue.arn
  function_name    = aws_lambda_function.main.arn
  enabled          = true
  batch_size       = 10
  depends_on       = [aws_lambda_function.main]
}
