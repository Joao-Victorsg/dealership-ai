resource "aws_sns_topic" "sales_topic" {
  name = "sales-topic"

  tags = {
    Name    = "sales-topic"
    Project = "dealership-ai"
  }
}
