locals {
  workflow_name = "invoice-workflow"
  tags = {
    Name    = local.workflow_name
    Project = "dealership-ai"
  }

  workflow_definition = templatefile("${path.module}/workflow-definition.json", {
    invoice_processor_lambda_arn = data.terraform_remote_state.invoice_processor.outputs.lambda_function_arn
    send_email_lambda_arn        = data.terraform_remote_state.send_email.outputs.lambda_function_arn
  })
}
