# API 목록 (야간 작업)

- 모든 응답은 `ApiResponse<T>` (`{ "success", "data", "error": { "code", "message" } }`) 형식이다.
- 모든 API는 외부 Issuer가 발급한 JWT(Bearer)가 필요하다. 서버는 JWT `sub`로 내부 사용자 ID를 찾고, 처음 보는 `sub`면
  사용자를 만든다(`@CurrentUserId`, DECISIONS D11).
- 시각은 ISO-8601 UTC(`2026-11-09T05:18:00Z`), 좌표는 WGS84 위도/경도.
- 실패 코드: 검증 실패 400 `INVALID_REQUEST`, 인증 없음 401 `UNAUTHORIZED`, 없음/타인 소유 404 `NOT_FOUND`,
  중복·상태 충돌 409 `CONFLICT`.

## user

| 메서드 | 경로 | 요청 | 응답 `data` |
|---|---|---|---|
| GET | `/api/v1/users/me` | — | `UserResponse { id, pushTokenRegistered }` |
| PUT | `/api/v1/users/me/push-token` | `{ pushToken: string(≤512) }` | 없음 |
