# EC2 배포

인증 결정 및 issuer 설정 전까지 기능 API는 사용할 수 없다(401).
현재 HTTP 구성은 Swagger 확인용이다. 실제 JWT 사용 전 도메인과 TLS 프록시를 구성해야 한다.
운영 데이터 저장 전 DB 백업/복구 절차도 별도로 준비해야 한다.

PR A(컨테이너/prod 보안)를 develop에 먼저 병합하고 PR B(ECR/CI)를 병합한다.
EC2에서는 이미지를 빌드하지 않는다.

배포를 활성화하기 **전에** 아래 1~3을 완료한다. PR 병합만으로 AWS 설정을 만들지는 않는다.

1. infra/terraform에서 terraform plan을 검토하고 apply한다. 기존 GitHub OIDC provider가
   있으면 github_oidc_provider_arn을 지정한다. 기존 EC2의 user_data는 변경하지 않는다.
2. GitHub Repository Actions variables를 설정한다.
   - AWS_REGION: Terraform region
   - AWS_DEPLOY_ROLE_ARN: github_deploy_role_arn output
   - ECR_REPOSITORY: ecr_repository_url output (태그 제외 전체 URL)
   - EC2_INSTANCE_ID: ec2_instance_id output
3. EC2 /opt/hmc/.env가 있어야 한다. 기존 bootstrap이 만든 DB 비밀번호를 유지한다.
   AWS CLI, Docker, Compose, curl이 설치되어 있고 SSM이 Online인지 확인한다.
   bootstrap_data_services=false인 외부 DB 구성은 이 compose 배포의 지원 대상이 아니다.
4. 마지막으로 Actions variable DEPLOY_ENABLED=true를 설정한다.
   기본(미설정)은 테스트만 실행하고 AWS 배포 job은 건너뛴다.
   develop push 또는 Actions Deploy 수동 실행으로 배포한다.
   linux/amd64 이미지를 ECR에 올리고 digest로 고정해 SSM에 전달한다.
   SSM 성공과 HTTP health 성공까지 확인해야 CI가 성공한다.

compose는 /opt/hmc/docker-compose.yml로 전달되며 기존 hmc-postgres, hmc-redis 및
hmc_postgres-data 볼륨을 사용한다. 다른 디렉터리/프로젝트 이름으로 기존 DB를
띄웠다면 먼저 볼륨 이름을 확인하고 compose를 맞춘다. down -v는 실행하지 않는다.
앱은 512MB 제한, JVM heap은 그중 최대 50%다. 실제 운영 부하에서 조정한다.

수동 실행/이전 이미지 재배포(SSM 또는 인스턴스 쉘):

```sh
cd /opt/hmc
# 저장소의 docker-compose.prod.yml을 docker-compose.yml로,
# scripts/deploy-ec2.sh를 deploy.sh로 복사한 뒤:
sudo bash deploy.sh '<ECR repository URL>@sha256:<digest>' ap-northeast-2
```

최근 성공 이미지는 /opt/hmc/.app-image에 기록한다. 다른 이미지로 되돌릴 때도
같은 스크립트에 이전 digest를 전달한다. DB 마이그레이션은 자동으로 되돌리지 않는다.
배포 실패는 CI에 전파하며 자동 rollback은 하지 않는다.
ECR은 최근 이미지 10개만 보관하므로 장기 보존할 복구 이미지는 별도 보관한다.

OAUTH2_ISSUER_URI가 비어 있으면 공개 Swagger/health만 접근 가능하고 다른 요청은
401이다. issuer를 설정하면 JWT를 검증한다. FIREBASE_CREDENTIALS_PATH와 LOKI_URL은
현재 초기화 코드가 없으므로 빈 값으로 기동 가능하다. 파일 기반 Firebase 자격증명을
추후 사용하려면 호스트 파일을 컨테이너 내부 경로로 마운트해야 한다.

OIDC 신뢰는 이 저장소 develop에 제한한다:
https://docs.aws.amazon.com/IAM/latest/UserGuide/id_roles_create_for-idp_oidc.html

평소 운영 명령은 성공 이미지 변수를 불러온 뒤 실행한다:

```sh
cd /opt/hmc
set -a
. ./.app-image
set +a
docker compose ps
docker compose logs --tail 100 app
```

CI는 PR과 develop push에서 check를 실행하고 통과 후에만 배포한다.
문서만 변경하면 실행하지 않는다. DEPLOY_ENABLED=true이면 develop 병합이 곧 운영 반영이다.
앱 배포는 app 이미지만 pull하며 데이터 서비스 이미지 갱신은 별도로 수행한다.
DB/Redis 메모리를 제한했지만 1GB 인스턴스의 성능을 보장하지 않는다.
데모/운영 부하에서 확인하고 필요하면 instance_type을 t3.small 이상으로 변경한다.
성공 후 72시간 이상 된 미사용 앱 이미지만 정리한다(다른 앱 이미지는 제외).
실패 시 compose 상태와 앱/DB/Redis 로그를 출력한다.
자동 rollback은 Flyway 적용 후 이전 앱의 스키마 호환성을 보장할 수 없어 수행하지 않는다.
DB 스키마와 호환되는 이전 digest를 확인하고 위 수동 재배포 절차를 사용한다.

컨테이너 로그는 서비스당 10MB 파일 3개로 제한한다(DB와 같은 디스크를 채우지 않게).
더 오래된 로그가 필요하면 Loki 등 외부로 보낸다.
배포 중에는 앱 컨테이너가 교체되어 수십 초 동안 요청이 실패한다(단일 인스턴스, 무중단 아님).
처리 중이던 요청은 graceful shutdown으로 마무리하도록 앱의 종료 유예 시간을 40초로 두었다.
로그 설정이 처음 반영되는 배포에서는 hmc-postgres, hmc-redis가 한 번 재생성된다(볼륨은 유지).
