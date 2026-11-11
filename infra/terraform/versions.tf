terraform {
  required_version = ">= 1.9"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # 비용 0원을 위해 로컬 state를 사용한다(.gitignore 처리). 팀 공유가 필요해지면 S3 backend로 이전한다. README 참고.
}
