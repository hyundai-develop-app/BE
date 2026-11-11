# 에러 코드 목록

실패 응답은 `{"success": false, "data": null, "error": {"code": "USER4041", "message": "..."}}` 형태입니다. 클라이언트는 `error.code`로만 분기하고, `message`는 표시용으로만 씁니다.

코드 형식은 `{DOMAIN}{HTTP 상태 3자리}{순번 1자리}`입니다. 규칙은 `CODE_CONVENTIONS.md` 2.3을 보세요. 이 표의 원본은 각 `*ErrorCode` enum이며, 코드를 추가하거나 바꾸면 이 표도 함께 고칩니다.

## COMMON — 공통 (`global/exception/CommonErrorCode`)

| 코드 | HTTP | 이름 | 언제 |
|---|---|---|---|
| COMMON4001 | 400 | INVALID_REQUEST | 요청 바디·파라미터 검증 실패, JSON 파싱 실패, 필수 파라미터 누락, 타입 불일치 |
| COMMON4002 | 400 | INVALID_COORDINATE | 위도(-90~90)·경도(-180~180) 범위 밖 |
| COMMON4011 | 401 | UNAUTHORIZED | JWT 없음/검증 실패 |
| COMMON4031 | 403 | FORBIDDEN | 권한 없음 |
| COMMON4041 | 404 | NOT_FOUND | 존재하지 않는 API 경로 |
| COMMON4051 | 405 | METHOD_NOT_ALLOWED | 지원하지 않는 HTTP 메서드 |
| COMMON4091 | 409 | CONFLICT | 동시 생성 등으로 DB 무결성 제약과 충돌 |
| COMMON4151 | 415 | UNSUPPORTED_MEDIA_TYPE | 지원하지 않는 Content-Type |
| COMMON5001 | 500 | INTERNAL_ERROR | 처리되지 않은 서버 오류 |

## USER — 사용자 (`user/exception/UserErrorCode`)

| 코드 | HTTP | 이름 | 언제 |
|---|---|---|---|
| USER4001 | 400 | INVALID_AUTH_SUBJECT | JWT subject가 비었거나 255자 초과 |
| USER4002 | 400 | INVALID_PUSH_TOKEN | 푸시 토큰이 비었거나 512자 초과 |
| USER4041 | 404 | USER_NOT_FOUND | 사용자 없음 |

## PARKINGLOT — 주차장 (`parkinglot/exception/ParkingLotErrorCode`)

| 코드 | HTTP | 이름 | 언제 |
|---|---|---|---|
| PARKINGLOT4001 | 400 | INVALID_PARKING_LOT | 주차장 이름·위치·입구 안내·무료 시간·총 면수가 올바르지 않음 |
| PARKINGLOT4002 | 400 | INVALID_FEE | 요금 단위 시간 1분 미만, 단위 요금 음수, 둘 중 하나만 입력 |
| PARKINGLOT4003 | 400 | INVALID_FORECAST | 만차율 0~100 밖, 주차장·대상 시각 누락 |
| PARKINGLOT4004 | 400 | INVALID_SEARCH_RADIUS | 주변 검색 반경이 1~5000m 밖 |
| PARKINGLOT4005 | 400 | INVALID_FORECAST_PERIOD | 예측 조회 시작 시각이 끝 시각보다 늦음 |
| PARKINGLOT4041 | 404 | PARKING_LOT_NOT_FOUND | 주차장 없음 (주차 기록·도착 모드에서 주차장을 지정할 때도 발생) |

## PLACE — 저장한 장소 (`place/exception/PlaceErrorCode`)

| 코드 | HTTP | 이름 | 언제 |
|---|---|---|---|
| PLACE4001 | 400 | INVALID_SAVED_PLACE | 장소 이름·위치가 올바르지 않음 |
| PLACE4041 | 404 | SAVED_PLACE_NOT_FOUND | 장소 없음(다른 사용자의 장소 포함) |
| PLACE4091 | 409 | DUPLICATE_PLACE_NAME | 같은 이름의 장소가 이미 있음 |

## PARKING — 주차 기록 (`parking/exception/ParkingErrorCode`)

| 코드 | HTTP | 이름 | 언제 |
|---|---|---|---|
| PARKING4001 | 400 | INVALID_PARKING_RECORD | 주차 위치·주차 시각 등 기록 정보가 올바르지 않음 |
| PARKING4002 | 400 | INVALID_SPOT_MEMO | 층·구역 메모가 비었거나 100자 초과 |
| PARKING4003 | 400 | INVALID_EXIT_TIME | 출차 시각 누락, 주차 시각보다 이름 |
| PARKING4041 | 404 | PARKING_RECORD_NOT_FOUND | 주차 기록 없음(다른 사용자의 기록 포함) |
| PARKING4042 | 404 | ACTIVE_PARKING_RECORD_NOT_FOUND | 진행 중인(출차 전) 주차 기록 없음 |
| PARKING4091 | 409 | ALREADY_EXITED | 이미 출차한 기록을 출차·수정 |

## ARRIVAL — 도착 모드 (`arrival/exception/ArrivalErrorCode`)

| 코드 | HTTP | 이름 | 언제 |
|---|---|---|---|
| ARRIVAL4001 | 400 | INVALID_ARRIVAL_SESSION | 목적지 이름·위치·예상 도착 시각 등이 올바르지 않음 |
| ARRIVAL4002 | 400 | INVALID_WAYPOINT | 경유 주차장 누락, 목적지 주차장을 경유지로 지정 |
| ARRIVAL4041 | 404 | ARRIVAL_SESSION_NOT_FOUND | 도착 모드 없음(다른 사용자의 세션 포함) |
| ARRIVAL4042 | 404 | ACTIVE_ARRIVAL_SESSION_NOT_FOUND | 진행 중인 도착 모드 없음 |
| ARRIVAL4091 | 409 | ALREADY_ENDED | 이미 끝난 도착 모드를 완료·취소·경유지 변경 |
