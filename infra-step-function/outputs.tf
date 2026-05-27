output "invoice_workflow_state_machine_arn" {
  value = aws_sfn_state_machine.invoice_workflow.arn
}

output "invoice_workflow_state_machine_name" {
  value = aws_sfn_state_machine.invoice_workflow.name
}
