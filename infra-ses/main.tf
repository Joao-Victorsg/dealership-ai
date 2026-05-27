variable "sender_email" {
  type    = string
  default = "noreply@example.com"
}

resource "aws_ses_email_identity" "invoice_sender" {
  email = var.sender_email
}
