data "aws_iam_policy_document" "assume_lambda" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["lambda.amazonaws.com"]
    }
  }
}
resource "aws_iam_role" "lambda" {
  name               = local.names.role
  assume_role_policy = data.aws_iam_policy_document.assume_lambda.json
  tags               = local.required_tags
}
data "aws_iam_policy_document" "lambda" {
  statement {
    sid = "LogsWrite"
    actions = [
      "logs:CreateLogGroup",
      "logs:CreateLogStream",
      "logs:PutLogEvents"
    ]
    resources = ["*"]
  }
  statement {
    sid = "SqsConsume"
    actions = [
      "sqs:ChangeMessageVisibility",
      "sqs:DeleteMessage",
      "sqs:GetQueueAttributes",
      "sqs:ReceiveMessage"
    ]
    resources = [data.aws_sqs_queue.source.arn]
  }
  statement {
    sid       = "ReadSecret"
    actions   = ["secretsmanager:GetSecretValue"]
    resources = [aws_secretsmanager_secret.keycloak.arn]
  }
}
resource "aws_iam_role_policy" "lambda" {
  name   = local.names.policy
  role   = aws_iam_role.lambda.id
  policy = data.aws_iam_policy_document.lambda.json
}
