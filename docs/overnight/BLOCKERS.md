# 막힌 항목

(진행을 막은 항목은 없음. 아래는 사람이 결정해야 해서 구현을 보류한 것들이다.)

## 사람 결정 필요 (구현 보류)

- **보안 설정(`SecurityFilterChain`)**: 인증 흐름이 TBD라 추가하지 않았다(DECISIONS D11). 지금은 Spring Boot 기본 보안이라
  JWT를 보내는 실제 클라이언트의 POST/PUT/DELETE 요청이 CSRF 토큰 없이 403이 될 수 있다. Issuer/클레임 구조를 정한 뒤
  stateless JWT 설정(CSRF 비활성, 세션 STATELESS, `/swagger-ui.html`·`/api-docs`·actuator 허용 범위)을 추가해야 한다.
- **폰 전송(FCM 푸시)**: Firebase 빈 구성과 자격 증명 경로가 없어 `parking_records.sent_to_phone_at`을 채우지 않는다(DECISIONS D10).
- **주차장·예측 데이터 적재 경로**: Write 서비스만 있고 공개 API는 없다(DECISIONS D8). 공공 주차장 API 배치, 관리자 API 등 방식 결정 필요.
