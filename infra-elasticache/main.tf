resource "aws_elasticache_subnet_group" "car_redis" {
  name       = "car-redis-subnet-group"
  subnet_ids = data.aws_subnets.private.ids

  tags = {
    Name    = "car-redis-subnet-group"
    Project = "dealership-ai"
  }
}

resource "aws_elasticache_cluster" "car_redis" {
  cluster_id           = "car-redis"
  engine               = "redis"
  engine_version       = "7.1"
  node_type            = "cache.t3.micro"
  num_cache_nodes      = 1
  parameter_group_name = "default.redis7"
  port                 = 6379
  subnet_group_name    = aws_elasticache_subnet_group.car_redis.name
  security_group_ids   = [aws_security_group.car_redis.id]

  tags = {
    Name    = "car-redis"
    Project = "dealership-ai"
  }
}

# ---------------------------------------------------------------------------
# client-api Redis
# ---------------------------------------------------------------------------

resource "aws_elasticache_subnet_group" "client_redis" {
  name       = "client-redis-subnet-group"
  subnet_ids = data.aws_subnets.private.ids

  tags = {
    Name    = "client-redis-subnet-group"
    Project = "dealership-ai"
  }
}

resource "aws_elasticache_cluster" "client_redis" {
  cluster_id           = "client-redis"
  engine               = "redis"
  engine_version       = "7.1"
  node_type            = "cache.t3.micro"
  num_cache_nodes      = 1
  parameter_group_name = "default.redis7"
  port                 = 6379
  subnet_group_name    = aws_elasticache_subnet_group.client_redis.name
  security_group_ids   = [aws_security_group.client_redis.id]

  tags = {
    Name    = "client-redis"
    Project = "dealership-ai"
  }
}

# ---------------------------------------------------------------------------
# sales-api Redis
# ---------------------------------------------------------------------------

resource "aws_elasticache_subnet_group" "sales_redis" {
  name       = "sales-redis-subnet-group"
  subnet_ids = data.aws_subnets.private.ids

  tags = {
    Name    = "sales-redis-subnet-group"
    Project = "dealership-ai"
  }
}

resource "aws_elasticache_cluster" "sales_redis" {
  cluster_id           = "sales-redis"
  engine               = "redis"
  engine_version       = "7.1"
  node_type            = "cache.t3.micro"
  num_cache_nodes      = 1
  parameter_group_name = "default.redis7"
  port                 = 6379
  subnet_group_name    = aws_elasticache_subnet_group.sales_redis.name
  security_group_ids   = [aws_security_group.sales_redis.id]

  tags = {
    Name    = "sales-redis"
    Project = "dealership-ai"
  }
}

# ---------------------------------------------------------------------------
# dealership-bff Redis
# ---------------------------------------------------------------------------

resource "aws_elasticache_subnet_group" "bff_redis" {
  name       = "bff-redis-subnet-group"
  subnet_ids = data.aws_subnets.private.ids

  tags = {
    Name    = "bff-redis-subnet-group"
    Project = "dealership-ai"
  }
}

resource "aws_elasticache_cluster" "bff_redis" {
  cluster_id           = "bff-redis"
  engine               = "redis"
  engine_version       = "7.1"
  node_type            = "cache.t3.micro"
  num_cache_nodes      = 1
  parameter_group_name = "default.redis7"
  port                 = 6379
  subnet_group_name    = aws_elasticache_subnet_group.bff_redis.name
  security_group_ids   = [aws_security_group.bff_redis.id]

  tags = {
    Name    = "bff-redis"
    Project = "dealership-ai"
  }
}
