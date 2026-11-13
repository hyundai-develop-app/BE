# 데모 JWT 사용법

외부 로그인 없이 API를 시연하기 위한 임시 발급 기능이다. `local` 프로필에서만 켜지며,
`prod`가 함께 활성화되어도 발급 API와 데모 서명 키는 등록되지 않는다.
운영은 기존 `OAUTH2_ISSUER_URI`를 그대로 사용한다.

## 실행

기존 `.env`의 DB·Redis 설정과 로컬 실행 준비를 마친 뒤 실행한다.
외부 Issuer 없이 실행할 수 있으며, local에서는 데모 검증기를 사용한다.
`OAUTH2_ISSUER_URI`가 설정되어 있어도 local에서는 외부 Issuer 토큰을 검증하지 않는다.

```sh
./gradlew bootRun --args='--spring.profiles.active=local'
```

## 토큰 발급과 API 호출

```sh
curl -X POST http://localhost:8080/api/v1/demo/token \
  -H 'Content-Type: application/json' \
  -d '{"subject":"demo-driver-1"}'
```

응답의 `data.accessToken`을 아래 요청의 `<토큰>`에 넣는다.
`data.tokenType`은 `Bearer`, `data.expiresAt`은 UTC 만료 시각이다.

```sh
curl http://localhost:8080/api/v1/users/me \
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
- 이 변경은 배포를 수행하지 않는다. `prod`로 실행 중인 서버에서는 데모 토큰을 발급하거나 사용할 수 없다.

## 제거

데모 종료 후 `LocalDemoSecurityConfig`, `DemoTokenIssuer`, `DemoTokenController`와 관련 DTO·문서·테스트를 제거하면 된다.
DB 스키마나 운영 보안 설정은 변경하지 않는다.
