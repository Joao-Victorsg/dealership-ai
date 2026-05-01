resource "aws_db_subnet_group" "car_db" {
  name       = "car-db-subnet-group"
  subnet_ids = data.aws_subnets.private.ids

  tags = {
    Name    = "car-db-subnet-group"
    Project = "dealership-ai"
  }
}

# Aurora Serverless v2 — PostgreSQL
resource "aws_rds_cluster" "car_cluster" {
  cluster_identifier   = "car-cluster"
  engine               = "aurora-postgresql"
  engine_version       = "16.4"
  database_name        = "dealershipdb"
  master_username      = data.aws_ssm_parameter.database_username.value
  master_password      = data.aws_secretsmanager_secret_version.database_password.secret_string
  db_subnet_group_name = aws_db_subnet_group.car_db.name
  skip_final_snapshot  = true
  deletion_protection  = false
  port = 4510

  vpc_security_group_ids = [aws_security_group.car_db.id]

  serverlessv2_scaling_configuration {
    min_capacity = 0.5
    max_capacity = 4.0
  }

  tags = {
    Name    = "car-cluster"
    Project = "dealership-ai"
  }
}

resource "aws_rds_cluster_instance" "car_instance" {
  cluster_identifier = aws_rds_cluster.car_cluster.id
  instance_class     = "db.serverless"
  engine             = aws_rds_cluster.car_cluster.engine
  engine_version     = aws_rds_cluster.car_cluster.engine_version

  tags = {
    Name    = "car-instance"
    Project = "dealership-ai"
  }
}

# ---------------------------------------------------------------------------
# client-api database
# ---------------------------------------------------------------------------

resource "aws_db_subnet_group" "client_db" {
  name       = "client-db-subnet-group"
  subnet_ids = data.aws_subnets.private.ids

  tags = {
    Name    = "client-db-subnet-group"
    Project = "dealership-ai"
  }
}

resource "aws_rds_cluster" "client_cluster" {
  cluster_identifier   = "client-cluster"
  engine               = "aurora-postgresql"
  engine_version       = "16.4"
  database_name        = "dealershipdb"
  master_username      = data.aws_ssm_parameter.client_database_username.value
  master_password      = data.aws_secretsmanager_secret_version.client_database_password.secret_string
  db_subnet_group_name = aws_db_subnet_group.client_db.name
  skip_final_snapshot  = true
  deletion_protection  = false
  port                 = 4512

  vpc_security_group_ids = [aws_security_group.client_db.id]

  serverlessv2_scaling_configuration {
    min_capacity = 0.5
    max_capacity = 4.0
  }

  tags = {
    Name    = "client-cluster"
    Project = "dealership-ai"
  }
}

resource "aws_rds_cluster_instance" "client_instance" {
  cluster_identifier = aws_rds_cluster.client_cluster.id
  instance_class     = "db.serverless"
  engine             = aws_rds_cluster.client_cluster.engine
  engine_version     = aws_rds_cluster.client_cluster.engine_version

  tags = {
    Name    = "client-instance"
    Project = "dealership-ai"
  }
}

# ---------------------------------------------------------------------------
# sales-api database
# ---------------------------------------------------------------------------

resource "aws_db_subnet_group" "sales_db" {
  name       = "sales-db-subnet-group"
  subnet_ids = data.aws_subnets.private.ids

  tags = {
    Name    = "sales-db-subnet-group"
    Project = "dealership-ai"
  }
}

resource "aws_rds_cluster" "sales_cluster" {
  cluster_identifier   = "sales-cluster"
  engine               = "aurora-postgresql"
  engine_version       = "16.4"
  database_name        = "salesdb"
  master_username      = data.aws_ssm_parameter.sales_database_username.value
  master_password      = data.aws_secretsmanager_secret_version.sales_database_password.secret_string
  db_subnet_group_name = aws_db_subnet_group.sales_db.name
  skip_final_snapshot  = true
  deletion_protection  = false
  port                 = 4513

  vpc_security_group_ids = [aws_security_group.sales_db.id]

  serverlessv2_scaling_configuration {
    min_capacity = 0.5
    max_capacity = 4.0
  }

  tags = {
    Name    = "sales-cluster"
    Project = "dealership-ai"
  }
}

resource "aws_rds_cluster_instance" "sales_instance" {
  cluster_identifier = aws_rds_cluster.sales_cluster.id
  instance_class     = "db.serverless"
  engine             = aws_rds_cluster.sales_cluster.engine
  engine_version     = aws_rds_cluster.sales_cluster.engine_version

  tags = {
    Name    = "sales-instance"
    Project = "dealership-ai"
  }
}
