resource "aws_cloudwatch_log_group" "car_api" {
  name              = "/ecs/car-api"
  retention_in_days = 14

  tags = {
    Name    = "car-api-logs"
    Project = "dealership-ai"
  }
}

resource "aws_ecs_task_definition" "car_api" {
  family                   = "car-api"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "1024"
  memory                   = "2048"
  execution_role_arn       = aws_iam_role.ecs_task_execution.arn

  container_definitions = jsonencode([
    {
      name      = "car-api"
      image     = "joaovictorsg/car-api-dealership:${var.image_tag}"
      cpu       = 1024
      memory    = 2048
      essential = true

      portMappings = [
        {
          containerPort = 8080
          hostPort      = 8080
          protocol      = "tcp"
        }
      ]

      environment = [
        { name = "SPRING_DATASOURCE_URL",      value = "jdbc:postgresql://${var.db_host}:${var.db_port}/${var.db_name}" },
        { name = "SPRING_DATASOURCE_USERNAME", value = var.db_username },
        { name = "SPRING_DATASOURCE_PASSWORD", value = var.db_password },
        { name = "REDIS_HOST",                 value = var.redis_host },
        { name = "REDIS_PORT",                 value = tostring(var.redis_port) },
        { name = "JWT_ISSUER_URI",             value = var.jwt_issuer_uri },
        { name = "S3_BUCKET",                  value = var.s3_bucket },
        { name = "S3_REGION",                  value = var.s3_region },
        { name = "S3_ENDPOINT",                value = var.s3_endpoint },
        { name = "S3_PRESIGNED_URL_TTL",       value = tostring(var.s3_presigned_url_ttl) },
        { name = "NEW_RELIC_LICENSE_KEY",      value = var.new_relic_license_key },
        { name = "NEW_RELIC_APP_NAME",         value = var.new_relic_app_name }
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.car_api.name
          "awslogs-region"        = "us-east-1"
          "awslogs-stream-prefix" = "car-api"
        }
      }

      healthCheck = {
        command     = ["CMD-SHELL", "wget -qO- http://localhost:8080/actuator/health || exit 1"]
        interval    = 30
        timeout     = 5
        retries     = 3
        startPeriod = 60
      }
    }
  ])

  tags = {
    Name    = "car-api-task"
    Project = "dealership-ai"
  }
}

resource "aws_security_group" "car_api" {
  name        = "car-api-task-sg"
  description = "Allow inbound traffic to car-api ECS tasks from within the VPC"
  vpc_id      = data.aws_vpc.vpc.id

  ingress {
    description = "App port from VPC (NLB forwards here)"
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = [data.aws_vpc.vpc.cidr_block]
  }

  egress {
    description = "Allow all outbound (DB, Redis, S3, ECR, CloudWatch)"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name    = "car-api-task-sg"
    Project = "dealership-ai"
  }
}

resource "aws_lb_target_group" "car_api" {
  name        = "car-api-tg"
  port        = 8080
  protocol    = "TCP"
  vpc_id      = data.aws_vpc.vpc.id
  target_type = "ip"

  health_check {
    enabled             = true
    protocol            = "HTTP"
    path                = "/actuator/health"
    port                = "8080"
    interval            = 30
    healthy_threshold   = 2
    unhealthy_threshold = 2
    timeout             = 6
  }

  tags = {
    Name    = "car-api-tg"
    Project = "dealership-ai"
  }
}

resource "aws_lb_listener" "car_api" {
  load_balancer_arn = data.aws_lb.nlb.arn
  port              = 8080
  protocol          = "TCP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.car_api.arn
  }

  depends_on = [aws_lb_target_group.car_api]
}

resource "aws_ecs_service" "car_api" {
  name            = "car-api"
  cluster         = data.aws_ecs_cluster.cluster.id
  task_definition = aws_ecs_task_definition.car_api.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    security_groups  = [aws_security_group.car_api.id]
    subnets          = data.aws_subnets.private.ids
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.car_api.arn
    container_name   = "car-api"
    container_port   = 8080
  }

  depends_on = [aws_lb_listener.car_api]
}
