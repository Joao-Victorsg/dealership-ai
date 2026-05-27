data "aws_iam_policy_document" "assume_states" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["states.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "states" {
  name               = "${local.workflow_name}-role"
  assume_role_policy = data.aws_iam_policy_document.assume_states.json
  tags               = local.tags
}

data "aws_iam_policy_document" "states" {
  statement {
    sid = "InvokeWorkflowLambdas"
    actions = [
      "lambda:InvokeFunction"
    ]
    resources = [
      data.terraform_remote_state.invoice_processor.outputs.lambda_function_arn,
      data.terraform_remote_state.send_email.outputs.lambda_function_arn
    ]
  }
}

resource "aws_iam_role_policy" "states" {
  name   = "${local.workflow_name}-policy"
  role   = aws_iam_role.states.id
  policy = data.aws_iam_policy_document.states.json
}
