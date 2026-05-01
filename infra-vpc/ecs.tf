resource "aws_ecs_cluster" "main" {
  name = "api-dealership-ai"

  setting {
    name  = "containerInsights"
    value = "enabled"
  }

  tags = {
    Name    = "api-dealership-ai"
    Project = "dealership-ai"
  }
}
