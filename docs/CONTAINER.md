# 앱 컨테이너

```sh
docker build -t hmc-be:local .
docker run --rm --memory 512m -p 8080:8080 \
  --env-file .env \
  -e DB_HOST=host.docker.internal -e REDIS_HOST=host.docker.internal \
  hmc-be:local
```

Docker Desktop에서 로컬 PostgreSQL/Redis를 사용하는 예시다.
Linux/EC2에서는 compose 네트워크의 서비스 이름을 사용한다.
컨테이너 기본 프로필은 prod이며 root가 아닌 UID 10001로 실행한다.
EC2(x86_64)에 보낼 이미지는 linux/amd64로 빌드한다.

prod는 Swagger(/swagger-ui.html, /swagger-ui/**, /api-docs/**)와
/actuator/health만 공개한다. issuer가 비어 있으면 인증 기능을 활성화하지 않고
보호된 요청은 401로 차단한다. issuer를 설정하면 OAuth2 Resource Server로 JWT를 검증한다.
현재 Firebase/Loki는 설정값만 있고 초기화 코드가 없어 빈 값이 허용된다.

2026-11-11 로컬 검증:
- ./gradlew check: 203 tests, failures/errors/skipped 0
- docker build 성공 (로컬 ARM64, EC2 AMD64 빌드는 CI에서 수행)
- PostGIS 16-3.4 + Redis 7, 앱 메모리 512MB로 docker run 성공
- OAUTH2_ISSUER_URI, FIREBASE_CREDENTIALS_PATH, LOKI_URL을 모두 빈 문자열로 전달
- /swagger-ui.html: 302, /swagger-ui/index.html: 200, /api-docs: 200
- /actuator/health: 200, status UP
- /actuator/info 및 /api/users/me: 401
- 확인 직후 앱 메모리 약 381MB. 부하 테스트 결과는 아니다.
