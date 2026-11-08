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
