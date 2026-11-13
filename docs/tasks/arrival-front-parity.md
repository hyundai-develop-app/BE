# 작업: arrival — 앱 동작과 맞추기(경유지 해제, 반경 700m, 목적지 주차장 추정, 예측 없을 때 추천)

> 이슈 #22. 프론트(마중 앱) `ArrivalEngine`/`ArrivalService`의 실제 동작을 백엔드가 같은 결과로 계산하게 한다.
> 앱 동작 요약:
> - 브리핑: 목적지 반경 **700m** 안 주차장을 가까운 순으로 찾고, 가장 가까운 곳을 "목적지 주차장(main)"으로 본다.
> - 대안: 수용 대수를 아는 곳끼리 main보다 큰 첫 번째 주차장, 없으면 main 다음으로 가까운 주차장 하나.
> - "되돌리기" 버튼으로 경유지를 해제하고 원래 목적지로 돌아간다.
> - 앱은 내비 목적지(이름·좌표)만 알고 백엔드 주차장 ID를 모른다.
>
> 현재 백엔드는 반경 500m, `destinationParkingLotId`가 없으면 목적지 주차장이 비고, **예측이 없는 주차장은 대안에서 빠져**
> 예측 데이터가 없는 지금은 추천이 항상 null이다. 경유지 해제 API도 없다.

## 배정
- 작업 ID: `arrival-front-parity`
- 선행 작업: `parkinglot-front-fields` (integrated 후 새 HEAD에서 시작)
- 작성자 허용 경로: `src/main/java/com/hmc/hmcbe/arrival`
- 검증 대상: `com.hmc.hmcbe.arrival.*`, `com.hmc.hmcbe.global.docs.*`
- 마이그레이션: 없음

## 변경 1 — 경유지 해제 API
- `DELETE /api/v1/arrivals/{sessionId}/waypoint` → 200 `ApiResponse<ArrivalSessionResponse>` (`waypointParkingLotId = null`)
- 엔티티: `ArrivalSession.clearWaypoint()` — ACTIVE가 아니면 `ARRIVAL4091 ALREADY_ENDED`. 경유지가 없어도 성공(멱등).
- 서비스: `ArrivalWriteService.clearWaypoint(Long userId, Long sessionId)` → `ArrivalSessionResponse`. 남의 세션·없는 세션은 `ARRIVAL4041`.
- 컨트롤러 메서드 `clearWaypoint`, Docs에 `@Operation(summary = "경유 주차장 해제", description = "브리핑 '되돌리기'. 경유지를 지우고 원래 목적지로 돌아간다. 경유지가 없어도 성공한다.")`,
  `@ApiErrorCodes({"ARRIVAL4041", "ARRIVAL4091"})`, `sessionId`에 `@Parameter(description = "도착 모드 세션 ID", example = "10")`.

## 변경 2 — 검색 반경 700m
- 대안·목적지 추정 검색 반경을 500 → **700m**로 바꾼다(`ArrivalReadServiceImpl.SEARCH_RADIUS_METERS`). 추정(변경 3)도 같은 값을 쓴다.
- `ArrivalOutlookResponse.betterAlternativeCount`의 `@Schema` 설명 "(반경 500m)"을 "(반경 700m)"로, `ArrivalControllerDocs.getBriefing` 설명의 "500m"도 고친다.

## 변경 3 — 목적지 주차장 추정
- `POST /api/v1/arrivals`에서 `destinationParkingLotId`가 null이면, 목적지 좌표 반경 700m 안에서 **가장 가까운 주차장**의 ID를
  `destinationParkingLotId`로 저장한다(`ParkingLotReadService.findNearby(목적지, 700, expectedArrivalAt)`의 첫 번째). 없으면 null 그대로.
- 값을 보낸 경우는 지금처럼 존재만 확인(`PARKINGLOT4041`)하고 그 값을 쓴다.
- 결과 `ArrivalSessionResponse.destinationParkingLotId`에 추정값이 담긴다. `StartArrivalRequest.destinationParkingLotId`의 `@Schema`
  설명에 "보내지 않으면 목적지 반경 700m 안 가장 가까운 주차장으로 정한다"를 덧붙인다.

## 변경 4 — 예측이 없을 때 추천 규칙(앱과 동일)
`ParkingRecommendationPolicy.recommend(Long destinationParkingLotId, List<NearbyParkingLotResponse> candidates)`
(후보는 목적지 기준 가까운 순, `distanceMeters`는 목적지에서의 거리)
1. **예측 규칙(기존)**: 목적지 예측이 있으면 그보다 만차율이 낮은 곳, 목적지 예측이 없으면 혼잡(CONGESTED)이 아닌 곳.
   예측이 없는 후보는 이 규칙에서 빠진다. 결과가 1개 이상이면 지금처럼 만차율·거리 순으로 반환하고 끝.
2. **대체 규칙(신규)**: 1의 결과가 비었고 **목적지 예측이 없을 때만**(목적지가 없거나 목적지 forecast가 null) 적용한다.
   목적지 예측이 있는데 더 나은 곳이 없으면 추천하지 않는다(지금과 같음).
   - 풀(pool): 목적지가 아니고 **forecast가 null인** 후보, 거리순.
   - 추천: 풀에서 `totalSpaces`가 있고 목적지 `totalSpaces`(있을 때)보다 큰 첫 번째. 목적지가 없거나 목적지 `totalSpaces`가 null이거나
     더 큰 곳이 없으면 풀의 첫 번째(가장 가까운 곳).
   - `alternatives`: 추천을 맨 앞에 두고 나머지 풀을 거리순으로 이어 붙인다. 풀이 비면 추천 null, 대안 빈 목록.
- `Recommendation(destination, recommended, alternatives)` 구조와 "alternatives는 recommended를 포함" 규칙은 유지한다.
- 목적지 판정: `destinationParkingLotId`와 같은 ID의 후보. 지금과 같다.
- `ParkingBriefingResponse.alternatives`의 `@Schema` 설명에 대체 규칙을 한 줄로 덧붙인다(예: "예측이 하나도 없으면 목적지보다 큰 주차장, 없으면 가장 가까운 다른 주차장을 추천한다").

## 테스트 작성자에게
- `ParkingRecommendationPolicyTest`: 예측 규칙 회귀 + 대체 규칙(큰 주차장 우선, 크기 모르면 가장 가까운 곳, 목적지 예측이 있으면 대체 규칙 미적용, CONGESTED 예측만 있는 후보는 풀에서 제외, 목적지 없음).
- `ArrivalSessionTest`: `clearWaypoint` 성공·멱등·종료 세션 409.
- 서비스 테스트: 목적지 추정(가장 가까운 곳, 반경 안에 없으면 null, 값을 보내면 추정하지 않음), 반경 700m 전달, `clearWaypoint`의 소유권 404.
- 컨트롤러 테스트(`@WebMvcTest`): `DELETE .../waypoint` 200·404·409.
- 기존 테스트는 삭제하지 않고 새 동작에 맞게 기대값만 고친다(예: 반경 500 → 700).

## 완료 기준
- [ ] 테스트 작성자의 테스트가 모두 통과한다
- [ ] `./gradlew check` 통과 (OpenApiDocumentTest 포함)
- [ ] 리뷰 VERDICT: PASS + 현재 SNAPSHOT 일치
- [ ] 통합 후 `./gradlew check` 통과, 상태 `integrated`

## 미결정 / 가정
- 앱은 대안을 1개만 보여 주지만 백엔드는 목록을 준다. 앱은 `recommended`만 쓰면 된다.
- 목적지 추정은 시작 시점에 한 번만 한다(세션에 저장). 이후 주차장이 추가돼도 바뀌지 않는다.
