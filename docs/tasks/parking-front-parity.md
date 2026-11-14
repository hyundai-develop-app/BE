# 작업: parking — 앱 동작과 맞추기(P단 주차장 추정, 요약에 주소·요금 문구)

> 이슈 #22. 프론트(마중 앱)는 기어 P에서 "브리핑에서 고른 주차장, 없으면 GPS 300m 안 가장 가까운 주차장"을 주차장으로 기록하고,
> 다음 D단에서 "○○에 1시간 40분 주차했어요. 요금은 10분 800원이에요"처럼 요금 문구와 주소를 쓴다.
> 현재 백엔드는 `parkingLotId`를 보내지 않으면 주차장을 비워 두고, 요약에 요금 문구·주소가 없다.

## 배정
- 작업 ID: `parking-front-parity`
- 선행 작업: `parkinglot-front-fields` (integrated 후 새 HEAD에서 시작. `ParkingLotResponse.address`, `feeDescription`을 쓴다)
- 작성자 허용 경로: `src/main/java/com/hmc/hmcbe/parking`
- 검증 대상: `com.hmc.hmcbe.parking.*`, `com.hmc.hmcbe.global.docs.*`
- 마이그레이션: 없음

## 변경 1 — P단 기록 시 주차장 추정
- `POST /api/v1/parking-records`에서 `parkingLotId`가 null이면, 주차 위치(GPS) 반경 **300m** 안에서 가장 가까운 주차장 ID로 기록한다
  (`ParkingLotReadService.findNearby(주차 위치, 300, 서버 현재 시각)`의 첫 번째). 없으면 null 그대로.
- 값을 보낸 경우는 지금처럼 존재만 확인(`PARKINGLOT4041`)하고 그 값을 쓴다.
- 반경은 `ParkingRecordWriteServiceImpl`의 상수 `PARKING_LOT_MATCH_RADIUS_METERS = 300`으로 둔다.
- `ParkRequest.parkingLotId`의 `@Schema` 설명에 "보내지 않으면 주차 위치 300m 안 가장 가까운 주차장으로 기록한다"를 덧붙이고,
  `ParkingRecordControllerDocs.park` 설명에도 한 줄 덧붙인다.

## 변경 2 — 주차 요약에 주소·요금 문구
- `ParkingSummaryResponse`에 `String parkingLotAddress`, `String feeDescription`을 **`parkingLotName` 바로 뒤에** 추가한다.
  주차장을 모르면 둘 다 null. 주차장의 해당 값이 null이어도 null.
  - `@Schema`: `parkingLotAddress` — "주차장 주소. 주차장을 모르거나 주소가 없으면 null", 예 `서울 강남구 영동대로 513 지하`
  - `@Schema`: `feeDescription` — "주차장 요금 안내 문구(앱이 그대로 읽어 준다). 모르면 null", 예 `10분 800원`
- 나머지 필드와 계산(경과·무료·남은 무료 시간)은 그대로다.

## 테스트 작성자에게
- 쓰기 서비스 테스트: 추정(가장 가까운 곳 선택, 반경 300 전달, 후보 없으면 null, 값을 보내면 추정하지 않고 `findNearby` 미호출), 추정 시각이 서버 `Clock` 시각인지.
- 읽기 서비스 테스트: 요약의 주소·요금 문구(주차장 있음/없음/값 null).
- 컨트롤러 테스트: 응답 JSON에 새 필드가 나오는지.
- 기존 테스트는 삭제하지 않고 새 생성자·동작에 맞게 고친다.

## 완료 기준
- [ ] 테스트 작성자의 테스트가 모두 통과한다
- [ ] `./gradlew check` 통과 (OpenApiDocumentTest 포함)
- [ ] 리뷰 VERDICT: PASS + 현재 SNAPSHOT 일치
- [ ] 통합 후 `./gradlew check` 통과, 상태 `integrated`

## 미결정 / 가정
- 앱의 "브리핑에서 고른 주차장"은 앱이 `parkingLotId`로 보낸다(경유지로 고른 주차장 ID). 서버는 진행 중인 도착 모드의 경유지를 자동으로 읽지 않는다(도메인 간 결합을 늘리지 않음).
