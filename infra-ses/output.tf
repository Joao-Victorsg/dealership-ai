output "sender_email" {
  value = aws_ses_email_identity.invoice_sender.email
}

output "sender_identity_arn" {
  value = aws_ses_email_identity.invoice_sender.arn
}
