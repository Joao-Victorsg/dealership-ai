data "terraform_remote_state" "sns" {
  backend = "local"

  config = {
    path = "${path.module}/../infra-sns/terraform.tfstate"
  }
}
