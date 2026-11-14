# infra/terraform

HMC-BE의 AWS 인프라를 Terraform으로 관리합니다. 파일 업로드용 S3와, 앱(Swagger 포함)을 올릴 EC2 1대 + 네트워크 구성입니다. EC2·EBS·퍼블릭 IPv4는 계정의 무료 혜택 범위를 넘으면 과금됩니다(아래 "무료 혜택과 비용 확인").

## 계정 제약 (2026-11-14 확인)

현재 계정은 새 방식 무료 플랜 프로젝트 계정이라 AWS가 관리하는 SCP가 걸려 있다. 루트 사용자도 우회할 수 없다.

- EC2/ECR/SSM은 **ap-southeast-2(시드니)에서만** 허용된다. 그래서 `region` 기본값이 시드니다.
- IAM 사용자 액세스 키는 EC2/ECR/S3가 막힌다. **콘솔 CloudShell**(로그인 역할)에서 Terraform을 실행한다.
- IAM OIDC 공급자가 막혀 GitHub Actions OIDC 배포를 쓸 수 없다. `enable_github_oidc_deploy` 기본값은 false이고,
  배포는 GHCR + EC2 자동 pull로 한다(`docs/DEPLOYMENT.md`).

## 만들어지는 리소스

| 파일 | 리소스 | 용도 | 비용 |
|---|---|---|---|
| `main.tf` | S3 버킷 `hmc-prod-uploads-<계정ID>` | 1:1 문의 이미지 (비공개, SSE-S3 암호화, HTTPS 전용, 버저닝 없음) | 저장 용량·요청·전송량에 따른 과금 (계정별 무료 혜택 확인) |
| `main.tf` | IAM 정책 `hmc-prod-app-s3` | 위 버킷의 객체 읽기·쓰기·삭제와 목록 조회만 허용 | 무료 |
| `main.tf` | IAM 사용자 `hmc-prod-app` | 서버 밖(로컬 등)에서 쓸 계정 (정책 연결) | 무료 |
| `network.tf` | VPC, 퍼블릭 서브넷 1개, 인터넷 게이트웨이, 라우트 테이블 | EC2가 놓일 전용 네트워크 | 무료 |
| `network.tf` | 보안 그룹 `hmc-prod-app` | 앱 포트만 `app_allowed_cidrs`에 개방. SSH는 기본 닫힘. 5432·6379는 규칙 없음 | 무료 |
| `ec2.tf` | EC2 `hmc-prod-app` (Amazon Linux 2023, gp3 암호화 볼륨, IMDSv2 필수) | 앱 서버 | 인스턴스 시간·EBS 용량 과금 (계정별 무료 혜택 확인) |
| `ec2.tf` | IAM 역할·인스턴스 프로파일 `hmc-prod-ec2` | 액세스 키 없이 S3 접근(위 S3 정책 재사용) + SSM 접속 | 무료 |
| `ec2.tf` | Elastic IP (`create_elastic_ip=true`일 때) | 재시작해도 유지되는 고정 퍼블릭 IP | 퍼블릭 IPv4 시간당 과금 |
| `user_data.sh.tftpl` | 첫 부팅 스크립트 | 스왑, Docker, Docker Compose 설치, `/opt/hmc/.env` 생성, PostGIS·Redis 컨테이너 기동 | - |

## EC2 접속과 배포

- **접속은 SSM Session Manager**를 씁니다(SSH 포트를 열지 않음). `terraform output ssm_session_command`로 명령을 확인합니다. AWS CLI와 session-manager-plugin이 필요합니다. SSH가 꼭 필요하면 `key_name`과 `ssh_allowed_cidrs`(본인 IP/32)를 지정합니다. `0.0.0.0/0`은 검증으로 막혀 있습니다.
- **S3 접근에는 액세스 키가 필요 없습니다.** 인스턴스 역할이 자격증명을 제공하고, 메타데이터 홉 제한을 2로 두어 Docker 컨테이너 안의 앱도 받을 수 있습니다. 따라서 EC2에서는 `AWS_ACCESS_KEY_ID` 등을 설정하지 않습니다.
- **부트스트랩이 하는 일은 여기까지입니다.** 앱 컨테이너(Dockerfile·이미지 빌드·배포 파이프라인)는 이 저장소에 아직 없어서 포함하지 않았습니다. 인스턴스에는 `/opt/hmc/.env`(DB 비밀번호 자동 생성, `OAUTH2_ISSUER_URI`·`FIREBASE_CREDENTIALS_PATH`·`LOKI_URL`은 비어 있음)와 DB·Redis 컨테이너만 준비됩니다. 부트스트랩 로그는 `/var/log/hmc-bootstrap.log`입니다.
- `.env`의 `DB_HOST`·`REDIS_HOST`는 `localhost`입니다. DB·Redis가 호스트의 127.0.0.1에만 열려 있으므로, 나중에 앱을 컨테이너로 띄울 때는 `network_mode: host`로 실행하거나 같은 compose 네트워크에 넣고 호스트 이름을 `postgres`/`redis`로 바꿔야 합니다.
- **Swagger 주소**는 `terraform output swagger_ui_url`입니다. 앱이 떠 있어야 열리며, 현재 코드에는 SecurityFilterChain이 없어 Spring Boot 기본 보안이 적용되므로 `/swagger-ui.html`과 `/api-docs`가 인증 없이 열리지 않을 수 있습니다. 이 부분은 앱 코드에서 따로 설정해야 합니다.
- 통신은 **HTTP**입니다. 도메인·HTTPS(ALB/ACM 또는 리버스 프록시)는 포함하지 않았습니다.

### 데이터 손실 주의

PostgreSQL 데이터는 이 인스턴스의 EBS 볼륨에 있습니다. 인스턴스를 교체(`-replace`)하거나 삭제하면 DB도 사라집니다. 그래서 `ami`와 `user_data` 변경은 `ignore_changes`로 무시해 실수로 교체되지 않게 했습니다. 중요한 데이터가 생기면 RDS로 옮기거나 정기 백업(`pg_dump` → S3)을 두세요. 이때 `bootstrap_data_services=false`로 바꿉니다.

## 일부러 넣지 않은 것

비용이 나가거나 지금 필요 없는 리소스입니다. 필요해지면 그때 추가합니다.

- NAT Gateway, ALB, ECS, RDS, ElastiCache: 상시 과금. DB와 Redis는 EC2 안의 Docker 컨테이너로 대체했습니다.
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

EC2에서는 인스턴스 역할을 쓰므로 키가 필요 없습니다. 아래는 EC2 밖(로컬 개발 등)에서 S3를 쓸 때만 해당합니다. 액세스 키는 state 파일에 평문으로 남지 않도록 Terraform이 만들지 않습니다. `apply` 후 직접 발급합니다.

```bash
aws iam create-access-key --user-name "$(terraform output -raw app_iam_user)"
```

발급된 키는 서버의 환경변수(`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`)로만 주입하고 저장소에 커밋하지 않습니다.

## 주의

- `terraform.tfstate`에 계정 정보가 들어가므로 `.gitignore`로 제외되어 있습니다. 잃어버리면 기존 리소스를 `terraform import`해야 하니 백업하세요.
- `.terraform.lock.hcl`은 커밋합니다(프로바이더 버전 고정).
- 문의 이미지 보관 기간을 제한하려면 `inquiry_image_expiration_days` 변수를 지정합니다. 기본값 0은 만료 없음입니다.

## 무료 혜택과 비용 확인

2025년 7월 15일 이후 신규 계정에는 크레딧 기반 Free Tier가 적용됩니다. 기존의 S3 12개월 무료 한도를 모든 계정에 적용할 수 없으며, 이 구성 자체가 비용 상한을 보장하지 않습니다. 배포 전에 계정의 플랜·크레딧·만료일을 확인하세요. [AWS 공식 안내](https://aws.amazon.com/free/free-tier-faqs/)를 참고하세요.

EC2 관련 확인 사항:

- `instance_type` 기본값은 `t3.micro`입니다. 프리 티어 대상 인스턴스 타입은 계정 가입 시점에 따라 다르므로 apply 전에 콘솔에서 확인하세요.
- 퍼블릭 IPv4는 사용 중인 주소마다 시간당 과금되며, Elastic IP도 마찬가지입니다. 무료 혜택이 적용되는지는 계정 플랜에 달려 있습니다. 비용이 걱정되면 `create_elastic_ip=false`로 두되, 그 경우 인스턴스를 중지/시작하면 IP가 바뀝니다.
- 메모리 1GB급 인스턴스에서 JVM 앱과 PostgreSQL, Redis를 함께 돌리면 부족할 수 있습니다. 스왑(기본 2GB)을 두었지만 느려질 수 있으니 부하를 보고 `instance_type`을 올리세요.
- `docker_compose_version` 기본값(2.29.7)의 릴리스 존재 여부는 이 PR에서 확인하지 못했습니다. 다운로드가 실패하면 부트스트랩이 중단되므로, apply 전에 [Docker Compose 릴리스 페이지](https://github.com/docker/compose/releases)에서 확인하세요.
- 이 구성의 `plan`/`apply`와 부트스트랩 스크립트는 실제 AWS에서 실행해 보지 않았습니다. 첫 apply 후 `/var/log/hmc-bootstrap.log`를 확인하세요.
