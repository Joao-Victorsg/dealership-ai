# Internet-facing Network Load Balancer
# Placed in public subnets to receive external traffic directly (no API Gateway).
resource "aws_lb" "nlb" {
  name               = "api-dealership-ai"
  load_balancer_type = "network"
  internal           = false
  subnets            = aws_subnet.public[*].id

  enable_deletion_protection = false

  tags = {
    Name    = "api-dealership-ai"
    Project = "dealership-ai"
  }
}
