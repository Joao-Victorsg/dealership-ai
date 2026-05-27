variable "aws_region" {
  type    = string
  default = "us-east-1"
}

variable "localstack_endpoint" {
  type    = string
  default = "http://localhost:4566"
}

variable "lambda_function_name" {
  type    = string
  default = "send-email"
}

variable "lambda_zip_path" {
  type    = string
  default = "../lambda.zip"
}

variable "lambda_timeout_seconds" {
  type    = number
  default = 30
}

variable "lambda_memory_size" {
  type    = number
  default = 256
}

variable "log_retention_in_days" {
  type    = number
  default = 14
}
