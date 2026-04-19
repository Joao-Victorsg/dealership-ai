data "aws_vpc" "vpc" {
  filter {
    name   = "tag:Name"
    values = ["vpc_dealership_ai"]
  }
}

data "aws_subnets" "private" {
  filter {
    name   = "tag:Name"
    values = ["aws_subnet_private"]
  }

  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.vpc.id]
  }
}

data "aws_lb" "nlb" {
  name = "api-dealership-ai"
}

data "aws_ecs_cluster" "cluster" {
  cluster_name = "api-dealership-ai"
}
