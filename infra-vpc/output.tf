output "vpc_id" {
  description = "VPC ID"
  value       = aws_vpc.vpc_dealership_ai.id
}

output "vpc_cidr_block" {
  description = "VPC CIDR block"
  value       = aws_vpc.vpc_dealership_ai.cidr_block
}

output "private_subnet_ids" {
  description = "IDs of private subnets"
  value       = aws_subnet.private[*].id
}

output "public_subnet_ids" {
  description = "IDs of public subnets"
  value       = aws_subnet.public[*].id
}

output "nlb_arn" {
  description = "NLB ARN"
  value       = aws_lb.nlb.arn
}

output "nlb_dns_name" {
  description = "NLB public DNS name"
  value       = aws_lb.nlb.dns_name
}

output "ecs_cluster_id" {
  description = "ECS cluster ID"
  value       = aws_ecs_cluster.main.id
}

output "ecs_cluster_name" {
  description = "ECS cluster name"
  value       = aws_ecs_cluster.main.name
}
