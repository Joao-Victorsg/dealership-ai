output "car_status_queue_arn" {
  description = "ARN of the car status queue"
  value       = aws_sqs_queue.car_status_queue.arn
}

output "car_status_queue_url" {
  description = "URL of the car status queue"
  value       = aws_sqs_queue.car_status_queue.id
}

output "invoice_queue_arn" {
  description = "ARN of the invoice queue"
  value       = aws_sqs_queue.invoice_queue.arn
}

output "invoice_queue_url" {
  description = "URL of the invoice queue"
  value       = aws_sqs_queue.invoice_queue.id
}

output "car_status_dlq_arn" {
  description = "ARN of the car status dead-letter queue"
  value       = aws_sqs_queue.car_status_dlq.arn
}

output "car_status_dlq_url" {
  description = "URL of the car status dead-letter queue"
  value       = aws_sqs_queue.car_status_dlq.id
}

output "invoice_dlq_arn" {
  description = "ARN of the invoice dead-letter queue"
  value       = aws_sqs_queue.invoice_dlq.arn
}

output "invoice_dlq_url" {
  description = "URL of the invoice dead-letter queue"
  value       = aws_sqs_queue.invoice_dlq.id
}
