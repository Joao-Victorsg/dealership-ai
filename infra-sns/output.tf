output "sales_topic_arn" {
  description = "SNS topic ARN for Car Sold Event fanout"
  value       = aws_sns_topic.sales_topic.arn
}
