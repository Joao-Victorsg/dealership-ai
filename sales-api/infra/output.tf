output "service_url" {
  description = "URL for the sales-api service via the NLB"
  value       = "http://${data.aws_lb.nlb.dns_name}:8082"
}

output "nlb_dns_name" {
  description = "DNS name of the Network Load Balancer"
  value       = data.aws_lb.nlb.dns_name
}

output "ecs_service_name" {
  description = "ECS service name"
  value       = aws_ecs_service.sales_api.name
}

output "task_definition_arn" {
  description = "ECS task definition ARN"
  value       = aws_ecs_task_definition.sales_api.arn
}

output "log_group_name" {
  description = "CloudWatch log group for ECS container logs"
  value       = aws_cloudwatch_log_group.sales_api.name
}
