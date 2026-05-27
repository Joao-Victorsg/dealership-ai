data "terraform_remote_state" "invoice_processor" {
  backend = "local"
  config = {
    path = "${path.module}/../lambda-invoice-processor/infra/terraform.tfstate"
  }
}

data "terraform_remote_state" "send_email" {
  backend = "local"
  config = {
    path = "${path.module}/../lambda-send-email/infra/terraform.tfstate"
  }
}
