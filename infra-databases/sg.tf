resource "aws_security_group" "car_db" {
  name        = "car-db-sg"
  description = "Allow PostgreSQL access from within the VPC"
  vpc_id      = data.aws_vpc.vpc.id

  ingress {
    description = "PostgreSQL from VPC"
    from_port   = 5432
    to_port     = 5432
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
    Name    = "car-db-sg"
    Project = "dealership-ai"
  }
}

resource "aws_security_group" "client_db" {
  name        = "client-db-sg"
  description = "Allow PostgreSQL access from within the VPC"
  vpc_id      = data.aws_vpc.vpc.id

  ingress {
    description = "PostgreSQL from VPC"
    from_port   = 5432
    to_port     = 5432
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
    Name    = "client-db-sg"
    Project = "dealership-ai"
  }
}

resource "aws_security_group" "sales_db" {
  name        = "sales-db-sg"
  description = "Allow PostgreSQL access from within the VPC"
  vpc_id      = data.aws_vpc.vpc.id

  ingress {
    description = "PostgreSQL from VPC"
    from_port   = 5432
    to_port     = 5432
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
    Name    = "sales-db-sg"
    Project = "dealership-ai"
  }
}
