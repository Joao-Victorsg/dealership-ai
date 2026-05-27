data "terraform_remote_state" "s3" {
  backend = "local"
  config = {
    path = "${path.module}/../../infra-s3/terraform.tfstate"
  }
}

data "terraform_remote_state" "ses" {
  backend = "local"
  config = {
    path = "${path.module}/../../infra-ses/terraform.tfstate"
  }
}
