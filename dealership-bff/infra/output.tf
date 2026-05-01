output "api_gateway_url" {
  description = "Invoke URL of the API Gateway V2 (entry point for the Front End)"
  value       = aws_apigatewayv2_stage.default.invoke_url
}

output "nlb_dns_name" {
  description = "DNS name of the Network Load Balancer"
  value       = data.aws_lb.nlb.dns_name
}

output "ecs_service_name" {
  description = "ECS service name"
  value       = aws_ecs_service.dealership_bff.name
}

output "task_definition_arn" {
  description = "ECS task definition ARN"
  value       = aws_ecs_task_definition.dealership_bff.arn
}

output "log_group_name" {
  description = "CloudWatch log group for ECS container logs"
  value       = aws_cloudwatch_log_group.dealership_bff.name
}

output "vpc_link_id" {
  description = "ID of the API Gateway V2 VPC Link"
  value       = aws_apigatewayv2_vpc_link.dealership_bff.id
}
