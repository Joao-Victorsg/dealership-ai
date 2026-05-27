data "aws_sqs_queue" "invoice_queue" {
  name = var.source_queue_name
}

data "terraform_remote_state" "step_function" {
  backend = "local"
  config = {
    path = "${path.module}/../../infra-step-function/terraform.tfstate"
  }
}
