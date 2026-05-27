resource "aws_cloudwatch_log_group" "dealership_web" {
  name              = "/ecs/dealership-web"
  retention_in_days = 14

  tags = {
    Name    = "dealership-web-logs"
    Project = "dealership-ai"
  }
}

resource "aws_ecs_task_definition" "dealership_web" {
  family                   = "dealership-web"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "1024"
  memory                   = "2048"
  execution_role_arn       = aws_iam_role.ecs_task_execution.arn

  container_definitions = jsonencode([
    {
      name      = "dealership-web"
      image     = "000000000000.dkr.ecr.us-east-1.localhost.localstack.cloud:4566/joaovictorsg/dealership-web:${var.image_tag}"
      cpu       = 1024
      memory    = 2048
      essential = true

      portMappings = [
        {
          containerPort = 3000
          hostPort      = 3000
          protocol      = "tcp"
        }
      ]

      environment = [
        { name = "NODE_ENV", value = "production" },
        { name = "PORT", value = "3000" },
        { name = "BFF_URL", value = var.bff_url },
        { name = "NEXT_PUBLIC_BFF_URL", value = var.next_public_bff_url },
        { name = "NEXT_PUBLIC_APP_URL", value = var.next_public_app_url },
        { name = "NEXT_PUBLIC_KEYCLOAK_URL", value = var.next_public_keycloak_url },
        { name = "NEXT_TELEMETRY_DISABLED", value = "1" }
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.dealership_web.name
          "awslogs-region"        = "us-east-1"
          "awslogs-stream-prefix" = "dealership-web"
        }
      }

      healthCheck = {
        command     = ["CMD-SHELL", "wget -qO- http://localhost:3000/ || exit 1"]
        interval    = 30
        timeout     = 5
        retries     = 3
        startPeriod = 60
      }
    }
  ])

  tags = {
    Name    = "dealership-web-task"
    Project = "dealership-ai"
  }
}

resource "aws_security_group" "dealership_web" {
  name        = "dealership-web-task-sg"
  description = "Allow inbound traffic to dealership-web ECS tasks from within the VPC"
  vpc_id      = data.aws_vpc.vpc.id

  ingress {
    description = "App port from VPC (NLB forwards here)"
    from_port   = 3000
    to_port     = 3000
    protocol    = "tcp"
    cidr_blocks = [data.aws_vpc.vpc.cidr_block]
  }

  egress {
    description = "Allow all outbound (BFF calls, ECR, CloudWatch)"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name    = "dealership-web-task-sg"
    Project = "dealership-ai"
  }
}

resource "aws_lb_target_group" "dealership_web" {
  name        = "dealership-web-tg"
  port        = 3000
  protocol    = "TCP"
  vpc_id      = data.aws_vpc.vpc.id
  target_type = "ip"

  health_check {
    enabled             = true
    protocol            = "HTTP"
    path                = "/"
    port                = "3000"
    interval            = 30
    healthy_threshold   = 2
    unhealthy_threshold = 2
    timeout             = 6
  }

  tags = {
    Name    = "dealership-web-tg"
    Project = "dealership-ai"
  }
}

resource "aws_lb_listener" "dealership_web" {
  load_balancer_arn = data.aws_lb.nlb.arn
  port              = 3000
  protocol          = "TCP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.dealership_web.arn
  }

  depends_on = [aws_lb_target_group.dealership_web]
}

resource "aws_ecs_service" "dealership_web" {
  name            = "dealership-web"
  cluster         = data.aws_ecs_cluster.cluster.id
  task_definition = aws_ecs_task_definition.dealership_web.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    security_groups  = [aws_security_group.dealership_web.id]
    subnets          = data.aws_subnets.private.ids
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.dealership_web.arn
    container_name   = "dealership-web"
    container_port   = 3000
  }

  depends_on = [aws_lb_listener.dealership_web]
}
