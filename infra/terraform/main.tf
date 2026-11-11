provider "aws" {
  region = var.region

  default_tags {
    tags = {
      Project     = var.project
      Environment = var.environment
      ManagedBy   = "terraform"
    }
  }
}

data "aws_caller_identity" "current" {}

locals {
  # S3 버킷 이름은 전 세계에서 유일해야 하므로 계정 ID를 붙인다.
  bucket_name = "${var.project}-${var.environment}-uploads-${data.aws_caller_identity.current.account_id}"
}

# ---------------------------------------------------------------------------
# S3: 1:1 문의 이미지 업로드용 (비용은 계정 혜택과 사용량에 따라 달라진다)
# ---------------------------------------------------------------------------
resource "aws_s3_bucket" "uploads" {
  bucket = local.bucket_name
}

resource "aws_s3_bucket_public_access_block" "uploads" {
  bucket = aws_s3_bucket.uploads.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_ownership_controls" "uploads" {
  bucket = aws_s3_bucket.uploads.id

  rule {
    object_ownership = "BucketOwnerEnforced"
  }
}

# SSE-S3(AES256): 추가 비용 없음. KMS는 요청당 과금이라 사용하지 않는다.
resource "aws_s3_bucket_server_side_encryption_configuration" "uploads" {
  bucket = aws_s3_bucket.uploads.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

# 버저닝은 저장 용량을 늘리므로 끈다. 삭제 정합성은 앱(AFTER_COMMIT + 정리 배치)이 맡는다.
resource "aws_s3_bucket_lifecycle_configuration" "uploads" {
  bucket = aws_s3_bucket.uploads.id

  rule {
    id     = "abort-incomplete-multipart"
    status = "Enabled"

    filter {}

    abort_incomplete_multipart_upload {
      days_after_initiation = 1
    }
  }

  dynamic "rule" {
    for_each = var.inquiry_image_expiration_days > 0 ? [1] : []

    content {
      id     = "expire-uploads"
      status = "Enabled"

      filter {}

      expiration {
        days = var.inquiry_image_expiration_days
      }
    }
  }
}

# HTTPS가 아닌 접근은 거부한다.
data "aws_iam_policy_document" "uploads_tls_only" {
  statement {
    sid     = "DenyInsecureTransport"
    effect  = "Deny"
    actions = ["s3:*"]
    resources = [
      aws_s3_bucket.uploads.arn,
      "${aws_s3_bucket.uploads.arn}/*",
    ]

    principals {
      type        = "*"
      identifiers = ["*"]
    }

    condition {
      test     = "Bool"
      variable = "aws:SecureTransport"
      values   = ["false"]
    }
  }
}

resource "aws_s3_bucket_policy" "uploads" {
  bucket = aws_s3_bucket.uploads.id
  policy = data.aws_iam_policy_document.uploads_tls_only.json

  depends_on = [aws_s3_bucket_public_access_block.uploads]
}

# ---------------------------------------------------------------------------
# IAM: 애플리케이션 전용 최소 권한 (해당 버킷의 객체 읽기/쓰기/삭제만)
# 액세스 키는 state에 비밀이 남지 않도록 Terraform이 만들지 않는다. README 참고.
# ---------------------------------------------------------------------------
data "aws_iam_policy_document" "app_s3" {
  statement {
    sid       = "ObjectReadWriteDelete"
    actions   = ["s3:GetObject", "s3:PutObject", "s3:DeleteObject"]
    resources = ["${aws_s3_bucket.uploads.arn}/*"]
  }

  statement {
    sid       = "ListBucket"
    actions   = ["s3:ListBucket"]
    resources = [aws_s3_bucket.uploads.arn]
  }
}

resource "aws_iam_policy" "app_s3" {
  name        = "${var.project}-${var.environment}-app-s3"
  description = "HMC-BE 서버가 업로드 버킷을 사용하기 위한 최소 권한"
  policy      = data.aws_iam_policy_document.app_s3.json
}

resource "aws_iam_user" "app" {
  name = "${var.project}-${var.environment}-app"
}

resource "aws_iam_user_policy_attachment" "app_s3" {
  user       = aws_iam_user.app.name
  policy_arn = aws_iam_policy.app_s3.arn
}
