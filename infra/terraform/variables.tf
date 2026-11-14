variable "region" {
  # 현재 계정(새 방식 무료 플랜 프로젝트 계정)은 AWS 관리 SCP가 시드니 외 리전의 EC2/ECR/SSM을 막는다.
  # SCP 제약이 없는 계정으로 옮기면 ap-northeast-2(서울)로 바꿔도 된다. README "계정 제약" 참고.
  description = "AWS 리전 (현재 계정 제약으로 시드니)"
  type        = string
  default     = "ap-southeast-2"
}

variable "project" {
  description = "리소스 이름 접두사"
  type        = string
  default     = "hmc"
}

variable "environment" {
  description = "환경 이름 (prod 단일 환경으로 시작)"
  type        = string
  default     = "prod"
}

variable "inquiry_image_expiration_days" {
  description = "1:1 문의 이미지 보관 기간(일). 0이면 만료하지 않는다."
  type        = number
  default     = 0

  validation {
    condition     = var.inquiry_image_expiration_days >= 0 && floor(var.inquiry_image_expiration_days) == var.inquiry_image_expiration_days
    error_message = "inquiry_image_expiration_days는 0 이상의 정수여야 합니다."
  }
}

# ---------------------------------------------------------------------------
# 네트워크 / EC2
# ---------------------------------------------------------------------------

variable "vpc_cidr" {
  description = "VPC CIDR"
  type        = string
  default     = "10.0.0.0/16"

  validation {
    condition     = can(cidrnetmask(var.vpc_cidr))
    error_message = "vpc_cidr은 올바른 IPv4 CIDR이어야 합니다."
  }
}

variable "public_subnet_cidr" {
  description = "퍼블릭 서브넷 CIDR (vpc_cidr 안에 있어야 한다)"
  type        = string
  default     = "10.0.1.0/24"

  validation {
    condition     = can(cidrnetmask(var.public_subnet_cidr))
    error_message = "public_subnet_cidr은 올바른 IPv4 CIDR이어야 합니다."
  }
}

variable "app_port" {
  description = "앱(API + Swagger UI)이 listen하는 포트. Spring Boot 기본값은 8080이다."
  type        = number
  default     = 8080

  validation {
    condition     = var.app_port >= 1 && var.app_port <= 65535 && floor(var.app_port) == var.app_port
    error_message = "app_port는 1~65535 사이의 정수여야 합니다."
  }
}

variable "app_allowed_cidrs" {
  description = "앱 포트에 접근을 허용할 CIDR 목록. 기본값은 전 세계 공개이므로, Swagger를 팀 내부에서만 볼 거면 사무실/집 IP(예: [\"203.0.113.10/32\"])로 좁힌다."
  type        = list(string)
  default     = ["0.0.0.0/0"]

  validation {
    condition     = alltrue([for c in var.app_allowed_cidrs : can(cidrnetmask(c))])
    error_message = "app_allowed_cidrs의 모든 값은 올바른 IPv4 CIDR이어야 합니다."
  }
}

variable "ssh_allowed_cidrs" {
  description = "SSH(22) 접근을 허용할 CIDR 목록. 기본값은 빈 목록(SSH 닫힘)이며 SSM Session Manager로 접속한다."
  type        = list(string)
  default     = []

  validation {
    condition     = alltrue([for c in var.ssh_allowed_cidrs : can(cidrnetmask(c))]) && !contains(var.ssh_allowed_cidrs, "0.0.0.0/0")
    error_message = "ssh_allowed_cidrs는 올바른 IPv4 CIDR이어야 하고, 0.0.0.0/0은 허용하지 않습니다."
  }
}

variable "key_name" {
  description = "SSH용 EC2 키 페어 이름. null이면 키 없이 만들고 SSM으로만 접속한다."
  type        = string
  default     = null
}

variable "instance_type" {
  description = "EC2 인스턴스 타입. 프리 티어 대상 여부는 계정 가입 시점에 따라 다르므로 apply 전에 확인한다."
  type        = string
  default     = "t3.micro"
}

variable "root_volume_size_gb" {
  description = "루트 EBS(gp3) 크기(GB). PostgreSQL 데이터도 여기에 저장된다."
  type        = number
  default     = 30

  validation {
    condition     = var.root_volume_size_gb >= 8 && floor(var.root_volume_size_gb) == var.root_volume_size_gb
    error_message = "root_volume_size_gb는 8 이상의 정수여야 합니다 (AL2023 AMI 기본 크기가 8GB)."
  }
}

variable "swap_size_gb" {
  description = "스왑 파일 크기(GB). 0이면 만들지 않는다. 메모리 1GB급 인스턴스에서 JVM과 DB를 함께 돌릴 때 필요하다."
  type        = number
  default     = 2

  validation {
    condition     = var.swap_size_gb >= 0 && floor(var.swap_size_gb) == var.swap_size_gb
    error_message = "swap_size_gb는 0 이상의 정수여야 합니다."
  }
}

variable "create_elastic_ip" {
  description = "Elastic IP를 붙일지 여부. true면 중지/시작해도 주소가 유지된다. 퍼블릭 IPv4는 사용 시간당 과금이 있다(README 참고)."
  type        = bool
  default     = true
}

variable "bootstrap_data_services" {
  description = "true면 첫 부팅 시 같은 인스턴스에 PostgreSQL(PostGIS)과 Redis를 Docker로 띄운다. RDS/ElastiCache 등을 쓸 거면 false."
  type        = bool
  default     = true
}

variable "docker_compose_version" {
  description = "설치할 Docker Compose 플러그인 버전 (v 접두사 없이). 실제 릴리스 존재 여부는 apply 전에 확인한다."
  type        = string
  default     = "2.29.7"
}
