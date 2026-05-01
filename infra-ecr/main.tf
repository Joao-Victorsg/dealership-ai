resource "aws_ecr_repository" "car_api" {
  name                 = "joaovictorsg/car-api-dealership"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Name    = "car-api-dealership"
    Project = "dealership-ai"
  }
}

resource "aws_ecr_repository" "client_api" {
  name                 = "joaovictorsg/client-api-dealership"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Name    = "client-api-dealership"
    Project = "dealership-ai"
  }
}

resource "aws_ecr_repository" "sales_api" {
  name                 = "joaovictorsg/sales-api-dealership"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Name    = "sales-api-dealership"
    Project = "dealership-ai"
  }
}

resource "aws_ecr_repository" "dealership_bff" {
  name                 = "joaovictorsg/dealership-bff"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Name    = "dealership-bff"
    Project = "dealership-ai"
  }
}
