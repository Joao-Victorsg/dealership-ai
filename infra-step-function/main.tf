resource "aws_sfn_state_machine" "invoice_workflow" {
  name       = local.workflow_name
  role_arn   = aws_iam_role.states.arn
  type       = "STANDARD"
  definition = local.workflow_definition
  tags       = local.tags

  depends_on = [aws_iam_role_policy.states]
}
