resource "aws_sqs_queue" "car_status_dlq" {
  name                       = "car-status-dlq"
  visibility_timeout_seconds = 30
  message_retention_seconds  = 345600

  tags = {
    Name    = "car-status-dlq"
    Project = "dealership-ai"
  }
}

resource "aws_sqs_queue" "invoice_dlq" {
  name                       = "invoice-dlq"
  visibility_timeout_seconds = 30
  message_retention_seconds  = 345600

  tags = {
    Name    = "invoice-dlq"
    Project = "dealership-ai"
  }
}

resource "aws_sqs_queue" "car_status_queue" {
  name                       = "car-status-queue"
  visibility_timeout_seconds = 30
  message_retention_seconds  = 345600
  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.car_status_dlq.arn
    maxReceiveCount     = 5
  })

  tags = {
    Name    = "car-status-queue"
    Project = "dealership-ai"
  }
}

resource "aws_sqs_queue" "invoice_queue" {
  name                       = "invoice-queue"
  visibility_timeout_seconds = 30
  message_retention_seconds  = 345600
  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.invoice_dlq.arn
    maxReceiveCount     = 5
  })

  tags = {
    Name    = "invoice-queue"
    Project = "dealership-ai"
  }
}

resource "aws_sqs_queue_policy" "car_status_queue_policy" {
  queue_url = aws_sqs_queue.car_status_queue.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid       = "AllowSalesTopicToSendMessage"
        Effect    = "Allow"
        Principal = { Service = "sns.amazonaws.com" }
        Action    = "sqs:SendMessage"
        Resource  = aws_sqs_queue.car_status_queue.arn
        Condition = {
          ArnEquals = {
            "aws:SourceArn" = data.terraform_remote_state.sns.outputs.sales_topic_arn
          }
        }
      }
    ]
  })
}

resource "aws_sqs_queue_policy" "invoice_queue_policy" {
  queue_url = aws_sqs_queue.invoice_queue.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid       = "AllowSalesTopicToSendMessage"
        Effect    = "Allow"
        Principal = { Service = "sns.amazonaws.com" }
        Action    = "sqs:SendMessage"
        Resource  = aws_sqs_queue.invoice_queue.arn
        Condition = {
          ArnEquals = {
            "aws:SourceArn" = data.terraform_remote_state.sns.outputs.sales_topic_arn
          }
        }
      }
    ]
  })
}

resource "aws_sns_topic_subscription" "car_status_queue" {
  topic_arn            = data.terraform_remote_state.sns.outputs.sales_topic_arn
  protocol             = "sqs"
  endpoint             = aws_sqs_queue.car_status_queue.arn
  raw_message_delivery = true

  depends_on = [aws_sqs_queue_policy.car_status_queue_policy]
}

resource "aws_sns_topic_subscription" "invoice_queue" {
  topic_arn            = data.terraform_remote_state.sns.outputs.sales_topic_arn
  protocol             = "sqs"
  endpoint             = aws_sqs_queue.invoice_queue.arn
  raw_message_delivery = true

  depends_on = [aws_sqs_queue_policy.invoice_queue_policy]
}
