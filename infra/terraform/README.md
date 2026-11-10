# infra/terraform

HMC-BE의 AWS 인프라를 Terraform으로 관리합니다. AWS 프리티어 계정 기준으로 **비용이 거의 0원인 최소 구성**만 둡니다.

## 만들어지는 리소스

| 리소스 | 용도 | 비용 |
|---|---|---|
| S3 버킷 `hmc-prod-uploads-<계정ID>` | 1:1 문의 이미지 (비공개, SSE-S3 암호화, HTTPS 전용, 버저닝 없음) | 프리티어 5GB / 월 GET 2만·PUT 2천 (12개월) |
| IAM 정책 `hmc-prod-app-s3` | 위 버킷의 객체 읽기·쓰기·삭제와 목록 조회만 허용 | 무료 |
| IAM 사용자 `hmc-prod-app` | 서버가 쓰는 계정 (정책 연결) | 무료 |

## 일부러 넣지 않은 것

비용이 나가거나 지금 필요 없는 리소스입니다. 필요해지면 그때 추가합니다.

- NAT Gateway, ALB, EC2·ECS, RDS, ElastiCache: 상시 과금. DB와 Redis는 로컬 docker-compose 또는 무료 외부 서비스를 씁니다.
- KMS 키, S3 버저닝, CloudFront: 키·요청·저장 용량 과금.
- 원격 state(S3 + DynamoDB): 혼자 쓰는 동안은 로컬 state로 충분합니다. 팀이 함께 `apply`하게 되면 S3 backend로 옮깁니다.

## 사용법

```bash
cd infra/terraform
aws sts get-caller-identity   # 자격증명이 유효한지 먼저 확인
terraform init
terraform plan
terraform apply
terraform output              # s3_bucket_name 등
```

`terraform output -raw s3_bucket_name` 값을 `.env`의 `AWS_S3_BUCKET`에 넣습니다.

## 액세스 키

액세스 키는 state 파일에 평문으로 남지 않도록 Terraform이 만들지 않습니다. `apply` 후 직접 발급합니다.

```bash
aws iam create-access-key --user-name hmc-prod-app
```

발급된 키는 서버의 환경변수(`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`)로만 주입하고 저장소에 커밋하지 않습니다.

## 주의

- `terraform.tfstate`에 계정 정보가 들어가므로 `.gitignore`로 제외되어 있습니다. 잃어버리면 기존 리소스를 `terraform import`해야 하니 백업하세요.
- `.terraform.lock.hcl`은 커밋합니다(프로바이더 버전 고정).
- 문의 이미지 보관 기간을 제한하려면 `inquiry_image_expiration_days` 변수를 지정합니다. 기본값 0은 만료 없음입니다.
