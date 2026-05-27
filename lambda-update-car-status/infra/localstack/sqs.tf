data "aws_sqs_queue" "dlq" {
  name = var.dlq_queue_name
}

data "aws_sqs_queue" "source" {
  name = var.source_queue_name
}
