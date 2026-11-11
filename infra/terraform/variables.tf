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

  validation {
    condition     = var.inquiry_image_expiration_days >= 0 && floor(var.inquiry_image_expiration_days) == var.inquiry_image_expiration_days
    error_message = "inquiry_image_expiration_days는 0 이상의 정수여야 합니다."
  }
}
