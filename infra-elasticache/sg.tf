resource "aws_security_group" "car_redis" {
  name        = "car-redis-sg"
  description = "Allow Redis access from within the VPC"
  vpc_id      = data.aws_vpc.vpc.id

  ingress {
    description = "Redis from VPC"
    from_port   = 6379
    to_port     = 6379
    protocol    = "tcp"
    cidr_blocks = [data.aws_vpc.vpc.cidr_block]
  }

  egress {
    description = "Allow all outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name    = "car-redis-sg"
    Project = "dealership-ai"
  }
}

resource "aws_security_group" "client_redis" {
  name        = "client-redis-sg"
  description = "Allow Redis access from within the VPC"
  vpc_id      = data.aws_vpc.vpc.id

  ingress {
    description = "Redis from VPC"
    from_port   = 6379
    to_port     = 6379
    protocol    = "tcp"
    cidr_blocks = [data.aws_vpc.vpc.cidr_block]
  }

  egress {
    description = "Allow all outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name    = "client-redis-sg"
    Project = "dealership-ai"
  }
}

resource "aws_security_group" "sales_redis" {
  name        = "sales-redis-sg"
  description = "Allow Redis access from within the VPC"
  vpc_id      = data.aws_vpc.vpc.id

  ingress {
    description = "Redis from VPC"
    from_port   = 6379
    to_port     = 6379
    protocol    = "tcp"
    cidr_blocks = [data.aws_vpc.vpc.cidr_block]
  }

  egress {
    description = "Allow all outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name    = "sales-redis-sg"
    Project = "dealership-ai"
  }
}

resource "aws_security_group" "bff_redis" {
  name        = "bff-redis-sg"
  description = "Allow Redis access from within the VPC"
  vpc_id      = data.aws_vpc.vpc.id

  ingress {
    description = "Redis from VPC"
    from_port   = 6379
    to_port     = 6379
    protocol    = "tcp"
    cidr_blocks = [data.aws_vpc.vpc.cidr_block]
  }

  egress {
    description = "Allow all outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name    = "bff-redis-sg"
    Project = "dealership-ai"
  }
}
