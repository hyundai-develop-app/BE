# EC2 배포

PR A(컨테이너/prod 보안)를 develop에 먼저 병합하고 PR B(ECR/CI)를 병합한다.
EC2에서는 이미지를 빌드하지 않는다.

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
4. develop push 또는 Actions Deploy 수동 실행으로 배포한다.
   linux/amd64 이미지를 ECR에 올리고 digest로 고정해 SSM에 전달한다.
   SSM 성공과 HTTP health 성공까지 확인해야 CI가 성공한다.

compose는 /opt/hmc/docker-compose.yml로 전달되며 기존 hmc-postgres, hmc-redis 및
hmc_postgres-data 볼륨을 사용한다. 다른 디렉터리/프로젝트 이름으로 기존 DB를
띄웠다면 먼저 볼륨 이름을 확인하고 compose를 맞춘다. down -v는 실행하지 않는다.
앱은 512MB 제한, JVM heap은 그중 최대 60%다. 실제 운영 부하에서 조정한다.

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
