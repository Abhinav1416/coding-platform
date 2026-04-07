resource "aws_sqs_queue" "match_watch_queue" {
  name = "match-watch-queue"

  tags = {
    Name = "${var.project_name}-match-watch-queue"
  }
}

resource "aws_sqs_queue" "match_result_queue" {
  name = "match-result-queue"

  tags = {
    Name = "${var.project_name}-match-result-queue"
  }
}

resource "aws_sqs_queue" "submission_queue" {
  name = "submission-queue"

  tags = {
    Name = "${var.project_name}-submission-queue"
  }
}

resource "aws_s3_bucket" "backend_storage" {
  bucket_prefix = "${var.project_name}-backend-storage-"
  force_destroy = true

  tags = {
    Name = "${var.project_name}-backend-storage"
  }
}

resource "aws_s3_bucket" "frontend_hosting" {
  bucket_prefix = "${var.project_name}-frontend-hosting-"
  force_destroy = true

  tags = {
    Name = "${var.project_name}-frontend-hosting"
  }
}

resource "aws_s3_bucket_public_access_block" "backend_storage_block" {
  bucket = aws_s3_bucket.backend_storage.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}