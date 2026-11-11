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

output "ec2_instance_id" {
  description = "앱 서버 인스턴스 ID (SSM 접속: aws ssm start-session --target <id>)"
  value       = aws_instance.app.id
}

output "ec2_public_ip" {
  description = "앱 서버 퍼블릭 IP"
  value       = local.app_public_ip
}

output "app_base_url" {
  description = "앱 기본 주소 (HTTP, 도메인/HTTPS 미적용)"
  value       = "http://${local.app_public_ip}:${var.app_port}"
}

output "swagger_ui_url" {
  description = "Swagger UI 주소 (application.yml의 springdoc 경로 기준). 앱이 실행 중이고 보안 설정이 허용해야 열린다."
  value       = "http://${local.app_public_ip}:${var.app_port}/swagger-ui.html"
}

output "ssm_session_command" {
  description = "SSM Session Manager 접속 명령 (AWS CLI + session-manager-plugin 필요)"
  value       = "aws ssm start-session --target ${aws_instance.app.id} --region ${var.region}"
}
