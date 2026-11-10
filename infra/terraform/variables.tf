variable "region" {
  description = "AWS 리전 (서울)"
  type        = string
  default     = "ap-northeast-2"
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
}
