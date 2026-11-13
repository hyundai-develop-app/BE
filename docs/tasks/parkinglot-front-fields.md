# 작업: parkinglot — 프론트 주차장 데이터 필드(주소·요금 문구·주말 전용) 추가

> 이슈 #22. 프론트(마중 앱)의 `assets/parking.json` 주차장 데이터를 백엔드가 그대로 담고 돌려줄 수 있게 한다.
> 앱 필드: `name, lat, lng, address, capacity, fee("10분 800원" 같은 자유 문구), freeMinutes, entrance, kind, weekendOnly`.
> 지금 백엔드에는 `address`, 자유 문구 요금, `weekendOnly`가 없다. 단위 요금(`feeUnitMinutes/feeUnitPrice`)만으로는
> "최초 2시간 3,000원", "주말·공휴일 운영 시간 내 무료" 같은 앱 문구를 표현할 수 없다.

## 배정
- 작업 ID: `parkinglot-front-fields`
- 선행 작업: 없음
- 작성자 허용 경로: `src/main/java/com/hmc/hmcbe/parkinglot`, `src/main/resources/db/migration/V3__add_parking_lot_front_fields.sql`
- 검증 대상: `com.hmc.hmcbe.parkinglot.*`, `com.hmc.hmcbe.global.docs.*` (테스트 작성자가 실제 패턴을 보고서에 적는다)
- 마이그레이션 파일: `V3__add_parking_lot_front_fields.sql`

## public API 변경
- `ParkingLot.create(String name, GeoPoint location, boolean indoor, String entranceGuide, int freeMinutes, ParkingFee fee, Integer totalSpaces, String address, String feeDescription, boolean weekendOnly)`
  - 기존 7개 인자 뒤에 `address`, `feeDescription`, `weekendOnly`를 덧붙인다. 기존 7인자 `create`는 **제거**한다(호출부는 모두 새 시그니처로 바꾼다).
  - getter: `getAddress()`, `getFeeDescription()`, `isWeekendOnly()`
- `ParkingLotResponse` record 끝에 `String address`, `String feeDescription`, `boolean weekendOnly`를 덧붙인다. `from(ParkingLot)`이 채운다.
- `RegisterParkingLotRequest` 끝에 `@Size(max = 255) String address`, `@Size(max = 255) String feeDescription`, `Boolean weekendOnly`(null이면 false)를 덧붙이고 `ParkingLotWriteServiceImpl.register`가 전달한다.
- `ParkingLotReadService.findNearby(GeoPoint center, int radiusMeters, Instant at)` 시그니처는 유지한다. 동작만 바뀐다(아래).

## 불변식
- `address`: null 허용. 값이 있으면 공백이 아닌 255자 이하. 위반 시 `PARKINGLOT4001 INVALID_PARKING_LOT`. (빈 문자열·공백 문자열은 null로 저장하지 말고 거부한다)
- `feeDescription`: null 허용. 값이 있으면 공백이 아닌 255자 이하. 위반 시 `PARKINGLOT4001`.
- `weekendOnly`: 기본 false.
- 기존 불변식은 그대로다.

## 주말 전용 주차장 필터
- `findNearby`는 기준 시각 `at`을 **Asia/Seoul** 시간대로 바꾼 요일이 토·일이 아니면 `weekendOnly = true` 주차장을 결과에서 뺀다. 토·일이면 포함한다. 공휴일은 판단하지 않는다(앱과 같음).
- 필터는 결과 개수 제한(최대 10개)보다 **먼저** 적용한다. 즉 리포지토리 네이티브 쿼리 `WHERE` 절에서 거른다
  (`ParkingLotRepository.findNearby`에 `boolean includeWeekendOnly` 파라미터를 추가하고 `(:includeWeekendOnly OR p.weekend_only = FALSE)` 조건).
- `findNearbyById`와 `getParkingLot`은 요일과 무관하게 조회한다(ID로 직접 지정한 경우).

## 테이블
```sql
ALTER TABLE parking_lots ADD COLUMN address         VARCHAR(255);
ALTER TABLE parking_lots ADD COLUMN fee_description VARCHAR(255);
ALTER TABLE parking_lots ADD COLUMN weekend_only    BOOLEAN NOT NULL DEFAULT FALSE;
```

## Swagger 문서
- `OpenApiDocumentTest`는 모든 DTO 필드에 `@Schema(description)`이 있어야 통과한다. 새 필드에도 설명·예시를 단다.
  - `address`: "주소. 모르면 null", 예 `서울 강남구 영동대로 513 지하`
  - `feeDescription`: "요금 안내 문구(앱이 그대로 읽어 준다). 모르면 null", 예 `10분 800원`
  - `weekendOnly`: "토·일에만 운영(공휴일은 판단하지 않음). 평일 주변 검색에서는 빠진다", 예 `false`

## 테스트 작성자에게
- 다른 도메인 테스트(`parking`, `arrival`)가 `new ParkingLotResponse(...)`, `ParkingLot.create(...)`, `ParkingLotFixture`를 직접 쓴다. 새 시그니처에 맞게 **수정**한다(삭제 금지). 의미가 없는 새 인자는 `null, null, false`로 채운다.
- 엔티티 단위 테스트: 새 필드 저장, 길이·공백 경계.
- 리포지토리 테스트(Testcontainers): 새 컬럼 매핑, `includeWeekendOnly` 필터, 필터가 LIMIT보다 먼저 적용되는지(평일에 주말 전용 주차장 때문에 일반 주차장이 잘리지 않음).
- 서비스 테스트: 평일(예: 2026-11-13 금요일 KST)·주말(2026-11-14 토요일 KST) `at`에 따른 결과. UTC 자정 근처 경계(예: 2026-11-13T15:30:00Z = 토요일 00:30 KST)에서 KST 기준으로 판단하는지.

## 완료 기준
- [ ] 테스트 작성자의 테스트가 모두 통과한다
- [ ] `./gradlew check` 통과 (Spotless, ArchUnit, Testcontainers, contextLoads의 ddl validate, OpenApiDocumentTest)
- [ ] 리뷰 VERDICT: PASS + 현재 SNAPSHOT 일치
- [ ] 통합 후 `./gradlew check` 통과, 상태 `integrated`

## 미결정 / 가정
- `kind`(건물/공영/임시)는 추가하지 않는다. 기존 `indoor`와 `feeDescription`으로 앱 문구를 만들 수 있고, 추천 규칙에서 쓰지 않는다.
- 앱의 `capacity = 0`(모름)은 `totalSpaces = null`로 저장한다(데모 적재 작업에서 처리).
