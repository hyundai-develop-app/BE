# 작업: global — 데모 환경 주차장 13곳 적재

> 이슈 #22. 백엔드에는 주차장 등록 API가 없고(DECISIONS D8) DB가 비어 있어, 앱을 연결하면 주변 주차장·브리핑·요약이 모두 빈 값이다.
> 앱의 시연 데이터(`assets/parking.json`, 앱 저장소 2026-11-09 기준)를 **데모 환경에서만** 적재한다. 운영 데이터 적재 경로는 이번 범위가 아니다.

## 배정
- 작업 ID: `demo-seed`
- 선행 작업: `parkinglot-front-fields` (integrated 후 새 HEAD에서 시작. `address`, `fee_description`, `weekend_only` 컬럼을 쓴다)
- 작성자 허용 경로: `src/main/java/com/hmc/hmcbe/global/config/DemoSeedConfig.java`, `src/main/resources/db/demo`
- 검증 대상: `com.hmc.hmcbe.global.config.DemoSeed*`
- 마이그레이션: `db/migration` 버전 파일 없음. 반복 실행 마이그레이션 `src/main/resources/db/demo/R__demo_parking_lots.sql`

## 동작
- 설정 `app.demo-seed.enabled`(관리자가 `application.yml`에 `${DEMO_SEED_ENABLED:false}`로 추가해 둠)가 `true`일 때만
  Flyway 위치에 `classpath:db/demo`를 **추가**한다. 기존 `classpath:db/migration`은 그대로 둔다.
  - 구현: `@Configuration` `DemoSeedConfig` + `@ConditionalOnProperty(name = "app.demo-seed.enabled", havingValue = "true")`인
    `FlywayConfigurationCustomizer` 빈. 기존 위치 배열에 `classpath:db/demo`를 덧붙인다. 프로필 제한은 두지 않는다.
- `R__demo_parking_lots.sql`: 아래 13곳을 넣는다. **이름이 같은 주차장이 이미 있으면 넣지 않는다**(`INSERT ... SELECT ... WHERE NOT EXISTS`).
  여러 번 실행해도 중복이 생기지 않아야 한다. `created_at`, `updated_at`은 `now()`.
- 열 매핑
  - `name` ← name, `location` ← `ST_SetSRID(ST_MakePoint(lng, lat), 4326)` (경도, 위도 순서 주의)
  - `address` ← address, `fee_description` ← fee, `free_minutes` ← freeMinutes
  - `entrance_guide` ← entrance, 빈 문자열이면 NULL
  - `total_spaces` ← capacity, 0이면 NULL
  - `indoor` ← kind가 `건물`이면 TRUE, 아니면 FALSE
  - `weekend_only` ← weekendOnly(없으면 FALSE)
  - `fee_unit_minutes`/`fee_unit_price` ← fee가 정확히 `N분 M원` 형태(쉼표 포함 가능)인 경우만 N, M. 그 밖에는 둘 다 NULL.
    해당: 코엑스(10, 800), 봉은사(5, 400), 롯데월드몰(10, 1000), 서울역 서부(10, 600), 강남역(5, 400), DDP(10, 900), IFC몰(10, 1000).

## 데이터 (앱 `parking.json` 원문)
```json
[
  {"name": "코엑스 주차장", "lat": 37.5116, "lng": 127.0598, "address": "서울 강남구 영동대로 513 지하", "capacity": 2000, "fee": "10분 800원", "freeMinutes": 30, "entrance": "건물 뒷편 영동대로 쪽 입구, 봉은사로에서 우회전", "kind": "건물"},
  {"name": "봉은사 공영주차장", "lat": 37.5146, "lng": 127.0575, "address": "서울 강남구 봉은사로 531", "capacity": 300, "fee": "5분 400원", "freeMinutes": 0, "entrance": "봉은사로에서 바로 진입, 넓음", "kind": "공영"},
  {"name": "롯데월드몰 주차장", "lat": 37.5131, "lng": 127.1040, "address": "서울 송파구 올림픽로 300 지하", "capacity": 2700, "fee": "10분 1,000원", "freeMinutes": 120, "entrance": "올림픽로에서 진입, 입구 2곳", "kind": "건물"},
  {"name": "잠실종합운동장 주차장", "lat": 37.5150, "lng": 127.0730, "address": "서울 송파구 올림픽로 25", "capacity": 1800, "fee": "경기일 1일 4,000원", "freeMinutes": 0, "entrance": "동문 진입, 경기 종료 후 출차 정체 심함", "kind": "공영"},
  {"name": "서울역 서부 주차장", "lat": 37.5530, "lng": 126.9690, "address": "서울 용산구 청파로 378", "capacity": 600, "fee": "10분 600원", "freeMinutes": 0, "entrance": "서부역 쪽 진입", "kind": "공영"},
  {"name": "강남역 공영주차장", "lat": 37.4965, "lng": 127.0290, "address": "서울 강남구 테헤란로 지하", "capacity": 350, "fee": "5분 400원", "freeMinutes": 0, "entrance": "테헤란로 이면도로 진입, 일방통행", "kind": "공영"},
  {"name": "경복궁 주차장", "lat": 37.5770, "lng": 126.9750, "address": "서울 종로구 사직로 161", "capacity": 240, "fee": "최초 2시간 3,000원", "freeMinutes": 0, "entrance": "사직로 서쪽 입구", "kind": "공영"},
  {"name": "DDP 지하주차장", "lat": 37.5660, "lng": 127.0105, "address": "서울 중구 을지로 281 지하", "capacity": 650, "fee": "10분 900원", "freeMinutes": 0, "entrance": "을지로 방면에서 진입", "kind": "건물"},
  {"name": "IFC몰 주차장", "lat": 37.5245, "lng": 126.9265, "address": "서울 영등포구 국제금융로 10 지하", "capacity": 1800, "fee": "10분 1,000원", "freeMinutes": 60, "entrance": "국제금융로 진입", "kind": "건물"},
  {"name": "남산 공영주차장", "lat": 37.5540, "lng": 126.9900, "address": "서울 중구 소파로", "capacity": 300, "fee": "최초 1시간 2,000원", "freeMinutes": 0, "entrance": "소파로 진입, 타워까지 도보 15분", "kind": "공영"},
  {"name": "현대백화점 판교점 주차장", "lat": 37.392799, "lng": 127.111991, "address": "경기 성남시 분당구 판교역로146번길 20", "capacity": 0, "fee": "최초 30분 무료, 초과 10분당 1,000원, 구매 금액별 1~5시간 무료", "freeMinutes": 30, "entrance": "", "kind": "건물"},
  {"name": "알파돔 임시 주차장", "lat": 37.394143, "lng": 127.110328, "address": "경기 성남시 분당구 판교역로146번길 알파돔타워", "capacity": 0, "fee": "주말·공휴일 운영 시간 내 무료", "freeMinutes": 0, "entrance": "", "kind": "임시", "weekendOnly": true},
  {"name": "테크원 임시 주차장", "lat": 37.395417, "lng": 127.112183, "address": "경기 성남시 분당구 분당내곡로 131 판교 테크원타워", "capacity": 0, "fee": "주말·공휴일 운영 시간 내 무료", "freeMinutes": 0, "entrance": "", "kind": "임시", "weekendOnly": true}
]
```

## 테스트 작성자에게
- `@SpringBootTest` + `PostgisContainerConfig` + `@TestPropertySource(properties = "app.demo-seed.enabled=true")`:
  13곳이 적재되고, 대표 값(코엑스 단위 요금 10/800·주소·입구, 판교점 `total_spaces` NULL·`entrance_guide` NULL·단위 요금 NULL, 알파돔 `weekend_only` TRUE, 좌표 위도/경도 순서)이 맞다.
  리포지토리(`ParkingLotRepository`)나 `JdbcTemplate`으로 확인한다.
- 같은 스크립트를 한 번 더 실행해도(예: `JdbcTemplate`으로 파일 내용을 다시 실행) 13곳 그대로다.
- 설정이 없으면(기본값) 주차장이 적재되지 않는다(별도 테스트 클래스, 기본 `test` 프로필).

## 완료 기준
- [ ] 테스트 작성자의 테스트가 모두 통과한다
- [ ] `./gradlew check` 통과
- [ ] 리뷰 VERDICT: PASS + 현재 SNAPSHOT 일치
- [ ] 통합 후 `./gradlew check` 통과, 상태 `integrated`

## 미결정 / 가정
- 예측 만차율 데이터는 넣지 않는다(시각이 고정된 값이라 시드로 적합하지 않음). 예측이 없을 때의 추천은 `arrival-front-parity`의 대체 규칙이 처리한다.
- 데모 서버에서 쓰려면 배포 환경변수 `DEMO_SEED_ENABLED=true`를 켜야 한다(배포 설정 변경은 사용자 확인 후).
