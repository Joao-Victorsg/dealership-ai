output "car_redis_endpoint" {
  description = "car-api ElastiCache Redis node address"
  value       = aws_elasticache_cluster.car_redis.cache_nodes[0].address
}

output "car_redis_port" {
  description = "car-api ElastiCache Redis port"
  value       = aws_elasticache_cluster.car_redis.port
}

output "client_redis_endpoint" {
  description = "client-api ElastiCache Redis node address"
  value       = aws_elasticache_cluster.client_redis.cache_nodes[0].address
}

output "client_redis_port" {
  description = "client-api ElastiCache Redis port"
  value       = aws_elasticache_cluster.client_redis.port
}

output "sales_redis_endpoint" {
  description = "sales-api ElastiCache Redis node address"
  value       = aws_elasticache_cluster.sales_redis.cache_nodes[0].address
}

output "sales_redis_port" {
  description = "sales-api ElastiCache Redis port"
  value       = aws_elasticache_cluster.sales_redis.port
}

output "bff_redis_endpoint" {
  description = "dealership-bff ElastiCache Redis node address"
  value       = aws_elasticache_cluster.bff_redis.cache_nodes[0].address
}

output "bff_redis_port" {
  description = "dealership-bff ElastiCache Redis port"
  value       = aws_elasticache_cluster.bff_redis.port
}
