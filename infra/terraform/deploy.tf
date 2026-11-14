resource "aws_ecr_repository" "app" {
  name                 = "${local.name_prefix}-app"
  image_tag_mutability = "IMMUTABLE"
  image_scanning_configuration {
    scan_on_push = true
  }
}

resource "aws_ecr_lifecycle_policy" "app" {
  repository = aws_ecr_repository.app.name
  policy = jsonencode({ rules = [{
    rulePriority = 1
    description  = "Keep the latest 10 images"
    selection    = { tagStatus = "any", countType = "imageCountMoreThan", countNumber = 10 }
    action       = { type = "expire" }
  }] })
}

resource "aws_iam_role_policy_attachment" "ec2_ecr" {
  role       = aws_iam_role.ec2.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
}

variable "github_repository" {
  type    = string
  default = "hyundai-develop-app/BE"
}

# 현재 계정은 SCP가 IAM OIDC 공급자를 막아 GitHub Actions가 AWS에 접근할 수 없다.
# 그래서 기본은 끄고, CI는 GHCR에 이미지를 올리며 EC2가 직접 받아 배포한다(docs/DEPLOYMENT.md).
# OIDC가 허용되는 계정에서만 true로 켠다.
variable "enable_github_oidc_deploy" {
  description = "true면 GitHub Actions OIDC 배포 역할(ECR push + SSM 명령)을 만든다."
  type        = bool
  default     = false
}

variable "github_oidc_provider_arn" {
  description = "Existing GitHub OIDC provider ARN; null creates one in this account"
  type        = string
  default     = null
}

resource "aws_iam_openid_connect_provider" "github" {
  count          = var.enable_github_oidc_deploy && var.github_oidc_provider_arn == null ? 1 : 0
  url            = "https://token.actions.githubusercontent.com"
  client_id_list = ["sts.amazonaws.com"]
}

resource "aws_iam_role" "github_deploy" {
  count = var.enable_github_oidc_deploy ? 1 : 0
  name  = "${local.name_prefix}-github-deploy"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Federated = coalesce(var.github_oidc_provider_arn, try(aws_iam_openid_connect_provider.github[0].arn, null)) }
      Action    = "sts:AssumeRoleWithWebIdentity"
      Condition = { StringEquals = {
        "token.actions.githubusercontent.com:aud" = "sts.amazonaws.com"
        "token.actions.githubusercontent.com:sub" = "repo:${var.github_repository}:ref:refs/heads/develop"
      } }
    }]
  })
}

resource "aws_iam_role_policy" "github_deploy" {
  count = var.enable_github_oidc_deploy ? 1 : 0
  role  = aws_iam_role.github_deploy[0].id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      { Effect = "Allow", Action = ["ecr:GetAuthorizationToken"], Resource = "*" },
      { Effect = "Allow", Action = ["ecr:BatchCheckLayerAvailability", "ecr:InitiateLayerUpload", "ecr:UploadLayerPart", "ecr:CompleteLayerUpload", "ecr:PutImage", "ecr:BatchGetImage", "ecr:GetDownloadUrlForLayer"], Resource = aws_ecr_repository.app.arn },
      { Effect = "Allow", Action = ["ssm:SendCommand"], Resource = [
        "arn:aws:ssm:${var.region}::document/AWS-RunShellScript",
        aws_instance.app.arn
      ] },
      { Effect = "Allow", Action = ["ssm:GetCommandInvocation"], Resource = "*" }
    ]
  })
}

output "ecr_repository_url" {
  value = aws_ecr_repository.app.repository_url
}
output "github_deploy_role_arn" {
  description = "enable_github_oidc_deploy=false면 null"
  value       = try(aws_iam_role.github_deploy[0].arn, null)
}
