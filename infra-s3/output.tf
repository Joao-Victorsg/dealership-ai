output "car_images_bucket_name" {
  value = aws_s3_bucket.car_images.id
}

output "car_images_bucket_arn" {
  value = aws_s3_bucket.car_images.arn
}

output "invoice_bucket_name" {
  value = aws_s3_bucket.invoices.id
}

output "invoice_bucket_arn" {
  value = aws_s3_bucket.invoices.arn
}
