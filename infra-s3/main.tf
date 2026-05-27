resource "aws_s3_bucket" "car_images" {
  bucket = "car-images"

  tags = {
    Name        = "car-images"
    Project     = "dealership-ai"
    Environment = "dev"
  }
}

resource "aws_s3_bucket" "invoices" {
  bucket = "invoices-bucket"

  tags = {
    Name        = "invoices-bucket"
    Project     = "dealership-ai"
    Environment = "dev"
  }
}

# Protect against accidental deletion of image data
resource "aws_s3_bucket_versioning" "car_images" {
  bucket = aws_s3_bucket.car_images.id

  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_versioning" "invoices" {
  bucket = aws_s3_bucket.invoices.id

  versioning_configuration {
    status = "Enabled"
  }
}

# All objects are accessed exclusively via presigned URLs — public access must be blocked
resource "aws_s3_bucket_public_access_block" "car_images" {
  bucket = aws_s3_bucket.car_images.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_public_access_block" "invoices" {
  bucket = aws_s3_bucket.invoices.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# CORS is required so that browsers can use presigned PUT/GET URLs directly
resource "aws_s3_bucket_cors_configuration" "car_images" {
  bucket = aws_s3_bucket.car_images.id

  cors_rule {
    allowed_headers = ["*"]
    allowed_methods = ["GET", "PUT", "POST"]
    allowed_origins = ["*"]
    expose_headers  = ["ETag"]
    max_age_seconds = 3000
  }
}

# Abort incomplete multipart uploads to avoid accumulating orphaned parts
resource "aws_s3_bucket_lifecycle_configuration" "car_images" {
  bucket = aws_s3_bucket.car_images.id

  rule {
    id     = "abort-incomplete-multipart-uploads"
    status = "Enabled"

    abort_incomplete_multipart_upload {
      days_after_initiation = 7
    }
  }
}
