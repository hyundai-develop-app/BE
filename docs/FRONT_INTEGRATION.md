# 프론트(마중 앱) 연동 가이드

대상: `hyundai-develop-app/demo-repository` (Android, `com.arrival.mode`). 2026-11-14 `main` 기준으로 앱 코드를 읽고 정리했다.
앱은 아직 API를 호출하지 않는다. 주차장은 `assets/parking.json`, 주차 기록은 SharedPreferences(`ParkingMemory`)에 있다.
아래 표대로 바꾸면 화면 흐름(01~05)을 그대로 두고 데이터만 서버로 옮길 수 있다. 요청/응답 상세와 에러 코드는 Swagger(`/swagger-ui.html` 또는 정적 HTML)를 본다.

## 0. 인증
- 데모 서버(`DEMO_AUTH_ENABLED=true`)에서 앱 시작 시 `POST /api/v1/demo/token` `{ "subject": "<기기 고유값>" }` → `data.accessToken`.
- 이후 모든 요청에 `Authorization: Bearer {accessToken}`. 토큰은 1시간 유효하고 서버 재시작 시 무효 → 401(`COMMON4011`)이면 다시 발급.
- 같은 `subject`면 같은 사용자다. 기기마다 고정값(예: 설치 시 만든 UUID)을 쓴다.

## 1. 이벤트 → API

| 앱 이벤트 (코드 위치) | 지금 앱 동작 | 호출할 API | 앱이 쓸 값 |
|---|---|---|---|
| 내비 목적지가 **바뀜** (`ArrivalService.onNavi`, `changed`) | 화면 01 | `POST /api/v1/arrivals` `{destinationName, latitude, longitude, expectedArrivalAt}` | `data.id`(세션 ID) 저장. `expectedArrivalAt` = 현재 시각 + `remainSec`. `destinationParkingLotId`는 보내지 않는다 → 서버가 목적지 700m 안 가장 가까운 주차장으로 정한다 |
| 01 대기 화면 갱신 | 고정 문구 | `GET /api/v1/arrivals/{id}/outlook` | `destinationParkingLot`(목적지 주차장·만차율), `destinationForecasts`(막대그래프), `betterAlternativeCount`("근처에 더 여유 있는 주차장 N곳") |
| 브리핑 조건 충족 (`ArrivalEngine.onDestination`: 2km 이하 / 10분 전 / GPS 1.5km) | `Catalog.near` 700m, main·alt 계산 | `GET /api/v1/arrivals/{id}/briefing` | `destinationParkingLot` = 앱의 main, `recommended` = 앱의 alt(없으면 null). 음성 문구는 `parkingLot.name`, `entranceGuide`, `freeMinutes`, `feeDescription`, `distanceMeters`로 만든다 |
| "응" / 대안 카드 탭 (`accept`) | 내비 경로 변경 | `PUT /api/v1/arrivals/{id}/waypoint` `{parkingLotId: recommended.parkingLot.id}` | 내비 `routeTo`는 지금처럼 앱이 한다(좌표는 `recommended.parkingLot.latitude/longitude`) |
| "되돌리기" (`undo`) | 원래 목적지로 | `DELETE /api/v1/arrivals/{id}/waypoint` | — |
| 내비 도착 (`ArrivalEvent.Arrived`) | 안내 음성 | `POST /api/v1/arrivals/{id}/complete` | — |
| 기어 P (`onParked`) | `ParkingMemory.save` | `POST /api/v1/parking-records` `{latitude, longitude, parkingLotId?}` | 경유지를 골랐으면 그 주차장 ID, 아니면 보내지 않는다 → 서버가 300m 안 가장 가까운 주차장으로 기록. `data.id`(기록 ID) 저장, `data.parkedAt`으로 "14:20 주차" |
| 층·구역 대답 (`onSpot`) | `saveSpot` | `PUT /api/v1/parking-records/{recordId}/spot-memo` `{spotMemo}` | 다시 말하면 같은 API로 정정 |
| 다음 기어 D (`onDrive`) | 경과·무료 시간 계산 후 `memory.clear()` | `GET /api/v1/parking-records/active/summary` → `POST /api/v1/parking-records/{recordId}/exit` | `parkingLotName`, `parkingLotAddress`, `elapsedMinutes`, `freeMinutes`, `remainingFreeMinutes`, `feeDescription`, `record.spotMemo`, `quickDestinations`(집/회사 카드) |
| 앱 재시작 | 메모리 상태 소실 | `GET /api/v1/arrivals/active`, `GET /api/v1/parking-records/active` | 404(`ARRIVAL4042`/`PARKING4042`)면 진행 중인 것이 없다 |

## 2. 앱 데이터 ↔ 서버 필드

| 앱 `ParkingLot` | 서버 `ParkingLotResponse` | 비고 |
|---|---|---|
| `name` | `name` | |
| `lat`, `lng` | `latitude`, `longitude` | |
| `address` | `address` | null 가능 |
| `capacity` (0 = 모름) | `totalSpaces` (null = 모름) | |
| `fee` (문구) | `feeDescription` | 앱이 그대로 읽어 준다. 계산용 단위 요금은 `feeUnitMinutes`/`feeUnitPrice`(없을 수 있음) |
| `freeMinutes` | `freeMinutes` | |
| `entrance` ("" = 없음) | `entranceGuide` (null = 없음) | |
| `kind == "건물"` | `indoor` | 공영/임시 구분은 없다 |
| `weekendOnly` | `weekendOnly` | 서버가 평일 주변 검색에서 이미 뺀다(KST 기준) |
| — | `id` | 경유지·주차 기록에 쓴다 |

## 3. 서버가 앱 규칙과 같게 계산하는 것
- 주변 검색 반경 700m(브리핑), 주차장 추정 300m(P단) — 앱 `Catalog.near` 기본값과 같다.
- 추천: 예측 만차율이 있으면 "목적지보다 만차율 낮은 곳"이 우선이다. 예측이 없으면 앱과 같이 "수용 대수를 아는 곳 중 목적지보다 큰 곳, 없으면 가장 가까운 다른 곳".
- 주말 전용 주차장(알파돔·테크원 임시 주차장)은 평일에 안 나온다. 공휴일은 판단하지 않는다(앱과 같음).

## 4. 데모 데이터
- 데모 서버에서 `DEMO_SEED_ENABLED=true`면 앱 `parking.json`의 13곳이 적재된다(`src/main/resources/db/demo/R__demo_parking_lots.sql`). 이름이 같은 주차장은 다시 넣지 않는다.
- 예측 만차율 데이터는 없다. 01 화면 그래프(`destinationForecasts`)는 빈 배열이고 `forecast`는 null이다. 앱은 빈 값일 때 그래프를 숨기거나 "예측 정보 없음"을 보여 줘야 한다.

## 5. 서버에 없는 것 (앱이 계속 담당)
- 내비 경로 변경(`NaviGateway.routeTo`), 음성 인식·발화, "응/아니" 판정, 층·구역 문장 추출(`Reply`) — 서버는 결과 문자열만 받는다.
- 폰 전송: 서버 FCM 전송은 아직 없다(이슈 #5).
