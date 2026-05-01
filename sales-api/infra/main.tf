resource "aws_cloudwatch_log_group" "sales_api" {
  name              = "/ecs/sales-api"
  retention_in_days = 14

  tags = {
    Name    = "sales-api-logs"
    Project = "dealership-ai"
  }
}

resource "aws_ecs_task_definition" "sales_api" {
  family                   = "sales-api"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "1024"
  memory                   = "2048"
  execution_role_arn       = aws_iam_role.ecs_task_execution.arn

  container_definitions = jsonencode([
    {
      name      = "sales-api"
      image     = "000000000000.dkr.ecr.us-east-1.localhost.localstack.cloud:4566/joaovictorsg/sales-api-dealership:${var.image_tag}"
      cpu       = 1024
      memory    = 2048
      essential = true

      portMappings = [
        {
          containerPort = 8082
          hostPort      = 8082
          protocol      = "tcp"
        }
      ]

      environment = [
        { name = "SERVER_PORT",           value = "8082" },
        { name = "DATASOURCE_URL",        value = "jdbc:postgresql://${var.db_host}:${var.db_port}/${var.db_name}" },
        { name = "DATASOURCE_USERNAME",   value = var.db_username },
        { name = "DATASOURCE_PASSWORD",   value = var.db_password },
        { name = "REDIS_HOST",            value = var.redis_host },
        { name = "REDIS_PORT",            value = tostring(var.redis_port) },
        { name = "JWKS_URI",              value = var.jwks_uri },
        { name = "SNS_TOPIC_ARN",         value = var.sns_topic_arn },
        { name = "AWS_REGION",            value = var.aws_region },
        { name = "NEW_RELIC_LICENSE_KEY", value = var.new_relic_license_key },
        { name = "NEW_RELIC_APP_NAME",    value = var.new_relic_app_name }
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.sales_api.name
          "awslogs-region"        = "us-east-1"
          "awslogs-stream-prefix" = "sales-api"
        }
      }

      healthCheck = {
        command     = ["CMD-SHELL", "wget -qO- http://localhost:8082/actuator/health || exit 1"]
        interval    = 30
        timeout     = 5
        retries     = 3
        startPeriod = 60
      }
    }
  ])

  tags = {
    Name    = "sales-api-task"
    Project = "dealership-ai"
  }
}

resource "aws_security_group" "sales_api" {
  name        = "sales-api-task-sg"
  description = "Allow inbound traffic to sales-api ECS tasks from within the VPC"
  vpc_id      = data.aws_vpc.vpc.id

  ingress {
    description = "App port from VPC (NLB forwards here)"
    from_port   = 8082
    to_port     = 8082
    protocol    = "tcp"
    cidr_blocks = [data.aws_vpc.vpc.cidr_block]
  }

  egress {
    description = "Allow all outbound (DB, Redis, SNS, ECR, CloudWatch)"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name    = "sales-api-task-sg"
    Project = "dealership-ai"
  }
}

resource "aws_lb_target_group" "sales_api" {
  name        = "sales-api-tg"
  port        = 8082
  protocol    = "TCP"
  vpc_id      = data.aws_vpc.vpc.id
  target_type = "ip"

  health_check {
    enabled             = true
    protocol            = "HTTP"
    path                = "/actuator/health"
    port                = "8082"
    interval            = 30
    healthy_threshold   = 2
    unhealthy_threshold = 2
    timeout             = 6
  }

  tags = {
    Name    = "sales-api-tg"
    Project = "dealership-ai"
  }
}

resource "aws_lb_listener" "sales_api" {
  load_balancer_arn = data.aws_lb.nlb.arn
  port              = 8082
  protocol          = "TCP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.sales_api.arn
  }

  depends_on = [aws_lb_target_group.sales_api]
}

resource "aws_ecs_service" "sales_api" {
  name            = "sales-api"
  cluster         = data.aws_ecs_cluster.cluster.id
  task_definition = aws_ecs_task_definition.sales_api.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    security_groups  = [aws_security_group.sales_api.id]
    subnets          = data.aws_subnets.private.ids
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.sales_api.arn
    container_name   = "sales-api"
    container_port   = 8082
  }

  depends_on = [aws_lb_listener.sales_api]
}
