# ─── ECS ──────────────────────────────────────────────────────────────────────

resource "aws_cloudwatch_log_group" "dealership_bff" {
  name              = "/ecs/dealership-bff"
  retention_in_days = 14

  tags = {
    Name    = "dealership-bff-logs"
    Project = "dealership-ai"
  }
}

resource "aws_ecs_task_definition" "dealership_bff" {
  family                   = "dealership-bff"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "1024"
  memory                   = "2048"
  execution_role_arn       = aws_iam_role.ecs_task_execution.arn

  container_definitions = jsonencode([
    {
      name      = "dealership-bff"
      image     = "000000000000.dkr.ecr.us-east-1.localhost.localstack.cloud:4566/joaovictorsg/dealership-bff:${var.image_tag}"
      cpu       = 1024
      memory    = 2048
      essential = true

      portMappings = [
        {
          containerPort = 8083
          hostPort      = 8083
          protocol      = "tcp"
        }
      ]

      environment = [
        { name = "REDIS_HOST",            value = var.redis_host },
        { name = "REDIS_PORT",            value = tostring(var.redis_port) },
        { name = "KEYCLOAK_BASE_URL",              value = var.keycloak_base_url },
        { name = "KEYCLOAK_REALM",                 value = var.keycloak_realm },
        { name = "KEYCLOAK_CLIENT_ID",             value = var.keycloak_client_id },
        { name = "KEYCLOAK_SYSTEM_CLIENT_ID",      value = var.keycloak_system_client_id },
        { name = "KEYCLOAK_SYSTEM_CLIENT_SECRET",  value = var.keycloak_system_client_secret },
        { name = "CAR_API_BASE_URL",      value = coalesce(var.car_api_base_url,    "http://${data.aws_lb.nlb.dns_name}:8080") },
        { name = "CLIENT_API_BASE_URL",   value = coalesce(var.client_api_base_url, "http://${data.aws_lb.nlb.dns_name}:8081") },
        { name = "SALES_API_BASE_URL",    value = coalesce(var.sales_api_base_url,  "http://${data.aws_lb.nlb.dns_name}:8082") },
        { name = "NEW_RELIC_LICENSE_KEY", value = var.new_relic_license_key },
        { name = "NEW_RELIC_APP_NAME",    value = var.new_relic_app_name }
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.dealership_bff.name
          "awslogs-region"        = "us-east-1"
          "awslogs-stream-prefix" = "dealership-bff"
        }
      }

      healthCheck = {
        command     = ["CMD-SHELL", "wget -qO- http://localhost:8083/actuator/health || exit 1"]
        interval    = 30
        timeout     = 5
        retries     = 3
        startPeriod = 60
      }
    }
  ])

  tags = {
    Name    = "dealership-bff-task"
    Project = "dealership-ai"
  }
}

resource "aws_security_group" "dealership_bff" {
  name        = "dealership-bff-task-sg"
  description = "Allow inbound traffic to dealership-bff ECS tasks from within the VPC"
  vpc_id      = data.aws_vpc.vpc.id

  ingress {
    description = "App port from VPC (NLB / VPC Link forwards here)"
    from_port   = 8083
    to_port     = 8083
    protocol    = "tcp"
    cidr_blocks = [data.aws_vpc.vpc.cidr_block]
  }

  egress {
    description = "Allow all outbound (Redis, downstream APIs, ECR, CloudWatch)"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name    = "dealership-bff-task-sg"
    Project = "dealership-ai"
  }
}

resource "aws_lb_target_group" "dealership_bff" {
  name        = "dealership-bff-tg"
  port        = 8083
  protocol    = "TCP"
  vpc_id      = data.aws_vpc.vpc.id
  target_type = "ip"

  health_check {
    enabled             = true
    protocol            = "HTTP"
    path                = "/actuator/health"
    port                = "8083"
    interval            = 30
    healthy_threshold   = 2
    unhealthy_threshold = 2
    timeout             = 6
  }

  tags = {
    Name    = "dealership-bff-tg"
    Project = "dealership-ai"
  }
}

resource "aws_lb_listener" "dealership_bff" {
  load_balancer_arn = data.aws_lb.nlb.arn
  port              = 8083
  protocol          = "TCP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.dealership_bff.arn
  }

  depends_on = [aws_lb_target_group.dealership_bff]
}

resource "aws_ecs_service" "dealership_bff" {
  name            = "dealership-bff"
  cluster         = data.aws_ecs_cluster.cluster.id
  task_definition = aws_ecs_task_definition.dealership_bff.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    security_groups  = [aws_security_group.dealership_bff.id]
    subnets          = data.aws_subnets.private.ids
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.dealership_bff.arn
    container_name   = "dealership-bff"
    container_port   = 8083
  }

  depends_on = [aws_lb_listener.dealership_bff]
}

# ─── API Gateway V2 ───────────────────────────────────────────────────────────

resource "aws_cloudwatch_log_group" "api_gw" {
  name              = "/aws/api-gateway/dealership-bff"
  retention_in_days = 14

  tags = {
    Name    = "dealership-bff-api-gw-logs"
    Project = "dealership-ai"
  }
}

resource "aws_security_group" "vpc_link" {
  name        = "dealership-bff-vpc-link-sg"
  description = "Security group for the API Gateway V2 VPC Link to the BFF NLB"
  vpc_id      = data.aws_vpc.vpc.id

  egress {
    description = "Allow outbound to NLB listener port"
    from_port   = 8083
    to_port     = 8083
    protocol    = "tcp"
    cidr_blocks = [data.aws_vpc.vpc.cidr_block]
  }

  tags = {
    Name    = "dealership-bff-vpc-link-sg"
    Project = "dealership-ai"
  }
}

resource "aws_apigatewayv2_vpc_link" "dealership_bff" {
  name               = "dealership-bff-vpc-link"
  security_group_ids = [aws_security_group.vpc_link.id]
  subnet_ids         = data.aws_subnets.private.ids

  tags = {
    Name    = "dealership-bff-vpc-link"
    Project = "dealership-ai"
  }
}

resource "aws_apigatewayv2_api" "dealership_bff" {
  name          = "dealership-bff-api"
  protocol_type = "HTTP"
  description   = "HTTP API Gateway for the Dealership BFF — entry point for the Front End"

  cors_configuration {
    allow_origins = ["*"]
    allow_methods = ["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"]
    allow_headers = ["Content-Type", "Authorization"]
    max_age       = 300
  }

  tags = {
    Name    = "dealership-bff-api"
    Project = "dealership-ai"
  }
}

resource "aws_apigatewayv2_integration" "dealership_bff" {
  api_id             = aws_apigatewayv2_api.dealership_bff.id
  integration_type   = "HTTP_PROXY"
  connection_id      = aws_apigatewayv2_vpc_link.dealership_bff.id
  connection_type    = "VPC_LINK"
  description        = "Forward all traffic to the dealership-bff via the NLB"
  integration_method = "ANY"
  integration_uri    = aws_lb_listener.dealership_bff.arn

  payload_format_version = "1.0"
}

resource "aws_apigatewayv2_route" "dealership_bff_proxy" {
  api_id    = aws_apigatewayv2_api.dealership_bff.id
  route_key = "ANY /{proxy+}"
  target    = "integrations/${aws_apigatewayv2_integration.dealership_bff.id}"
}

resource "aws_apigatewayv2_route" "dealership_bff_root" {
  api_id    = aws_apigatewayv2_api.dealership_bff.id
  route_key = "ANY /"
  target    = "integrations/${aws_apigatewayv2_integration.dealership_bff.id}"
}

resource "aws_apigatewayv2_stage" "default" {
  api_id      = aws_apigatewayv2_api.dealership_bff.id
  name        = "$default"
  auto_deploy = true

  access_log_settings {
    destination_arn = aws_cloudwatch_log_group.api_gw.arn
    format = jsonencode({
      requestId      = "$context.requestId"
      ip             = "$context.identity.sourceIp"
      requestTime    = "$context.requestTime"
      httpMethod     = "$context.httpMethod"
      routeKey       = "$context.routeKey"
      status         = "$context.status"
      protocol       = "$context.protocol"
      responseLength = "$context.responseLength"
    })
  }
}
