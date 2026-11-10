output "s3_bucket_name" {
  description = "AWS_S3_BUCKET 환경변수에 넣을 값"
  value       = aws_s3_bucket.uploads.bucket
}

output "region" {
  description = "AWS_REGION 환경변수에 넣을 값"
  value       = var.region
}

output "app_iam_user" {
  description = "액세스 키를 발급할 IAM 사용자 (콘솔 또는 aws iam create-access-key)"
  value       = aws_iam_user.app.name
}
