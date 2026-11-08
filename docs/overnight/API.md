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

## parkinglot (조회 전용 — 등록/예측 적재 API는 없음, DECISIONS D8)

`ParkingLotResponse { id, name, latitude, longitude, indoor, entranceGuide, freeMinutes, feeUnitMinutes?, feeUnitPrice?, totalSpaces? }`
`OccupancyForecastResponse { targetAt, occupancyRate(0~100), congestionLevel: AVAILABLE|NORMAL|CONGESTED }`
`NearbyParkingLotResponse { parkingLot: ParkingLotResponse, distanceMeters, forecast: OccupancyForecastResponse? }`

| 메서드 | 경로 | 요청 | 응답 `data` |
|---|---|---|---|
| GET | `/api/v1/parking-lots/{parkingLotId}` | — | `ParkingLotResponse` |
| GET | `/api/v1/parking-lots/nearby` | query `latitude`, `longitude`, `at`(기준 시각, 필수), `radius`(m, 1~5000, 기본 500) | `NearbyParkingLotResponse[]` (가까운 순, 최대 10) |
| GET | `/api/v1/parking-lots/{parkingLotId}/forecasts` | query `from`, `to` | `OccupancyForecastResponse[]` (시각 순) |

## place (빠른 목적지)

`SavedPlaceResponse { id, name, latitude, longitude }`

| 메서드 | 경로 | 요청 | 응답 `data` |
|---|---|---|---|
| GET | `/api/v1/saved-places` | — | `SavedPlaceResponse[]` (저장 순) |
| POST | `/api/v1/saved-places` | `{ name: string(≤50), latitude, longitude }` | 201 `SavedPlaceResponse` (같은 이름이면 409) |
| DELETE | `/api/v1/saved-places/{savedPlaceId}` | — | 없음 |

## arrival (도착 모드, 화면 ①②③)

`ArrivalSessionResponse { id, destinationName, latitude, longitude, destinationParkingLotId?, waypointParkingLotId?, expectedArrivalAt, status: ACTIVE|COMPLETED|CANCELLED }`
`ArrivalOutlookResponse { session, destinationParkingLot: NearbyParkingLotResponse?, destinationForecasts: OccupancyForecastResponse[], betterAlternativeCount }`
`ParkingBriefingResponse { destinationParkingLot?, recommended?, alternatives: NearbyParkingLotResponse[] }`

| 메서드 | 경로 | 요청 | 응답 `data` |
|---|---|---|---|
| POST | `/api/v1/arrivals` | `{ destinationName(≤100), latitude, longitude, destinationParkingLotId?, expectedArrivalAt }` | 201 `ArrivalSessionResponse` (진행 중 세션은 취소됨) |
| GET | `/api/v1/arrivals/active` | — | `ArrivalSessionResponse` (없으면 404) |
| GET | `/api/v1/arrivals/{sessionId}/outlook` | — | `ArrivalOutlookResponse` (① 대기) |
| GET | `/api/v1/arrivals/{sessionId}/briefing` | — | `ParkingBriefingResponse` (② 브리핑) |
| PUT | `/api/v1/arrivals/{sessionId}/waypoint` | `{ parkingLotId }` | `ArrivalSessionResponse` (③ 경유지, 끝난 세션이면 409) |
| POST | `/api/v1/arrivals/{sessionId}/complete` | — | 없음 (끝난 세션이면 409) |
| POST | `/api/v1/arrivals/{sessionId}/cancel` | — | 없음 (끝난 세션이면 409) |

## parking (P단 주차 기록, 화면 ④⑤)

`ParkingRecordResponse { id, parkingLotId?, latitude, longitude, parkedAt, spotMemo?, exitedAt? }`
`ParkingSummaryResponse { record, parkingLotName?, elapsedMinutes, freeMinutes?, remainingFreeMinutes?, quickDestinations: SavedPlaceResponse[] }`

| 메서드 | 경로 | 요청 | 응답 `data` |
|---|---|---|---|
| POST | `/api/v1/parking-records` | `{ latitude, longitude, parkingLotId? }` | 201 `ParkingRecordResponse` (④-1, 주차 시각 = 서버 현재 시각, 이전 미출차 기록은 출차 처리) |
| GET | `/api/v1/parking-records/active` | — | `ParkingRecordResponse` (없으면 404) |
| GET | `/api/v1/parking-records/active/summary` | — | `ParkingSummaryResponse` (⑤ 다음 시동 요약) |
| PUT | `/api/v1/parking-records/{recordId}/spot-memo` | `{ spotMemo: string(≤100) }` | `ParkingRecordResponse` (④-2 층·구역 저장/정정, 출차 후면 409) |
| POST | `/api/v1/parking-records/{recordId}/exit` | — | 없음 (이미 출차면 409) |
