output "car_cluster_endpoint" {
  description = "car-api Aurora cluster writer endpoint"
  value       = aws_rds_cluster.car_cluster.endpoint
}

output "car_reader_endpoint" {
  description = "car-api Aurora cluster reader endpoint"
  value       = aws_rds_cluster.car_cluster.reader_endpoint
}

output "car_port" {
  description = "car-api Aurora cluster port"
  value       = aws_rds_cluster.car_cluster.port
}

output "car_database_name" {
  description = "car-api database name"
  value       = aws_rds_cluster.car_cluster.database_name
}

output "client_cluster_endpoint" {
  description = "client-api Aurora cluster writer endpoint"
  value       = aws_rds_cluster.client_cluster.endpoint
}

output "client_reader_endpoint" {
  description = "client-api Aurora cluster reader endpoint"
  value       = aws_rds_cluster.client_cluster.reader_endpoint
}

output "client_port" {
  description = "client-api Aurora cluster port"
  value       = aws_rds_cluster.client_cluster.port
}

output "client_database_name" {
  description = "client-api database name"
  value       = aws_rds_cluster.client_cluster.database_name
}

output "sales_cluster_endpoint" {
  description = "sales-api Aurora cluster writer endpoint"
  value       = aws_rds_cluster.sales_cluster.endpoint
}

output "sales_reader_endpoint" {
  description = "sales-api Aurora cluster reader endpoint"
  value       = aws_rds_cluster.sales_cluster.reader_endpoint
}

output "sales_port" {
  description = "sales-api Aurora cluster port"
  value       = aws_rds_cluster.sales_cluster.port
}

output "sales_database_name" {
  description = "sales-api database name"
  value       = aws_rds_cluster.sales_cluster.database_name
}
