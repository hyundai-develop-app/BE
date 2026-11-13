# 데모 JWT 사용법

외부 로그인 없이 API를 시연하기 위한 임시 발급 기능이다. `local`과 `prod` 프로필에서
`DEMO_AUTH_ENABLED=true`를 명시해야 켜진다. 기본값은 false다.
값은 `true`로 지정한다. `yes`, `on`, `1`은 데모 활성화 값으로 인정하지 않는다.

## AWS develop 배포에서 활성화

`develop`은 배포할 Git 브랜치이고, `prod`는 서버의 Spring 실행 프로필이다.
현재 `docker-compose.prod.yml`은 prod로 실행하며 서버 `/opt/hmc/.env`를 컨테이너에 전달한다.
프로필이나 Compose 파일을 바꿀 필요 없이 해당 서버의 `.env`에 아래 값을 추가한다.

```dotenv
DEMO_AUTH_ENABLED=true
```

PR 병합 후 기존 develop 배포가 완료되면 위 환경변수가 적용된 인스턴스에서 데모 발급을 사용할 수 있다.
이미 배포된 이미지에 환경변수만 바꾸는 경우 컨테이너를 재생성해야 한다.
기존 배포 절차에서 이미지가 `APP_IMAGE`로 전달되는 것과 동일하게 실행한다.

```sh
cd /opt/hmc
set -a
. ./.app-image
set +a
docker compose up -d --force-recreate app
```

`.app-image`는 기존 배포 스크립트가 배포 성공 후 기록하는 이미지 참조다.
첫 배포 전이라면 기존 develop 배포를 사용한다.
이 PR 자체는 서버 설정 변경, 배포 또는 브랜치 병합을 수행하지 않는다.

## 로컬 실행

기존 `.env`의 DB·Redis 설정과 로컬 실행 준비를 마친 뒤, `.env`의
`DEMO_AUTH_ENABLED`를 true로 바꾸고 실행한다.

```sh
./gradlew bootRun --args='--spring.profiles.active=local'
```

활성화 시 외부 Issuer 없이 실행할 수 있고, `OAUTH2_ISSUER_URI`가 설정되어 있어도
외부 Issuer 토큰 대신 데모 토큰만 검증한다. 비활성화 시 기존 외부 Issuer 인증을 사용한다.

## 토큰 발급과 API 호출

아래 주소를 실제 AWS 서버 주소로 바꾼다. 로컬은 `http://localhost:8080`을 사용한다.

```sh
curl -X POST https://<데모서버>/api/v1/demo/token \
  -H 'Content-Type: application/json' \
  -d '{"subject":"demo-driver-1"}'
```

응답의 `data.accessToken`을 아래 요청의 `<토큰>`에 넣는다.
`data.tokenType`은 `Bearer`, `data.expiresAt`은 UTC 만료 시각이다.

```sh
curl https://<데모서버>/api/v1/users/me \
  -H 'Authorization: Bearer <토큰>'
```

- `subject`는 공백이 아닌 1~255자다. 같은 값을 사용하면 기존 사용자로 이어지고,
  다른 값을 사용하면 다른 사용자로 시연할 수 있다. 최초 사용자 생성은 기존 `@CurrentUserId` 흐름에서 수행한다.
- 토큰 유효기간은 1시간이다. 만료되면 같은 subject로 다시 발급받는다.
- RS256 키는 시작 시 생성하고 메모리에만 보관한다. 서버를 재시작하면 기존 토큰을 다시 발급받아야 한다.
  여러 서버 인스턴스는 키를 공유하지 않으므로 단일 인스턴스로 시연한다.
- Swagger에서도 `/api/v1/demo/token`을 호출할 수 있다.
- 브라우저 프론트엔드는 `CORS_ALLOWED_ORIGINS`에 정확한 출처를 지정한다.
- 발급 API는 로그인 없이 지정한 사용자의 토큰을 만든다. 데모 인스턴스의 접근을 시연 참가자로 제한하고, 실제 사용자 데이터가 없는 별도 데모 DB를 사용한다.
- 데모 종료 시 `DEMO_AUTH_ENABLED=false`로 바꾸고 컨테이너를 재생성한다. 발급 API가 사라지고 데모 JWT는 더 이상 인증되지 않는다. 외부 인증을 사용하려면 유효한 `OAUTH2_ISSUER_URI`를 설정한다.

## 제거

데모 종료 후 `DemoJwtConfig`, `DemoTokenIssuer`, `DemoTokenController`와 관련 DTO·문서·테스트를 제거하면 된다.
`ProdSecurityConfig`의 데모 활성화 분기도 제거한다. DB 스키마 변경은 없다.
