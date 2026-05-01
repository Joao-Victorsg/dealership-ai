data "aws_availability_zones" "available" {
  state = "available"
}

resource "aws_vpc" "vpc_dealership_ai" {
  cidr_block           = "10.32.0.0/16"
  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = {
    Name    = "vpc_dealership_ai"
    Project = "dealership-ai"
  }
}

# ---------------------------------------------------------------------------
# Private subnets — ECS tasks, databases, cache
# ---------------------------------------------------------------------------

resource "aws_subnet" "private" {
  count             = 3
  cidr_block        = cidrsubnet(aws_vpc.vpc_dealership_ai.cidr_block, 8, count.index)
  availability_zone = data.aws_availability_zones.available.names[count.index]
  vpc_id            = aws_vpc.vpc_dealership_ai.id

  tags = {
    Name    = "aws_subnet_private"
    Project = "dealership-ai"
    Tier    = "private"
  }
}

resource "aws_route_table" "private" {
  count  = 3
  vpc_id = aws_vpc.vpc_dealership_ai.id

  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.nat[count.index].id
  }

  tags = {
    Name    = "rt-private-${count.index}"
    Project = "dealership-ai"
  }
}

resource "aws_route_table_association" "private" {
  count          = 3
  subnet_id      = aws_subnet.private[count.index].id
  route_table_id = aws_route_table.private[count.index].id
}

# ---------------------------------------------------------------------------
# Public subnets — NAT Gateways, internet-facing NLB
# ---------------------------------------------------------------------------

resource "aws_subnet" "public" {
  count                   = 3
  cidr_block              = cidrsubnet(aws_vpc.vpc_dealership_ai.cidr_block, 8, 3 + count.index)
  availability_zone       = data.aws_availability_zones.available.names[count.index]
  vpc_id                  = aws_vpc.vpc_dealership_ai.id
  map_public_ip_on_launch = true

  tags = {
    Name    = "aws_subnet_public"
    Project = "dealership-ai"
    Tier    = "public"
  }
}

resource "aws_internet_gateway" "igw" {
  vpc_id = aws_vpc.vpc_dealership_ai.id

  tags = {
    Name    = "igw-dealership-ai"
    Project = "dealership-ai"
  }
}

resource "aws_eip" "nat" {
  count  = 3
  domain = "vpc"

  depends_on = [aws_internet_gateway.igw]

  tags = {
    Name    = "eip-nat-${count.index}"
    Project = "dealership-ai"
  }
}

resource "aws_nat_gateway" "nat" {
  count         = 3
  subnet_id     = aws_subnet.public[count.index].id
  allocation_id = aws_eip.nat[count.index].id

  depends_on = [aws_internet_gateway.igw]

  tags = {
    Name    = "nat-${count.index}"
    Project = "dealership-ai"
  }
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.vpc_dealership_ai.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.igw.id
  }

  tags = {
    Name    = "rt-public"
    Project = "dealership-ai"
  }
}

resource "aws_route_table_association" "public" {
  count          = 3
  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}
