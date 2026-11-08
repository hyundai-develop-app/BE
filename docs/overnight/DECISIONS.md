# 자율 작업 결정 기록

## D1. Figma URL이 없을 때 S1을 추측으로 진행하지 않는다 (2026-11-09)

> 해소됨: 올바른 Figma URL로 재실행한다. 이 결정(추측으로 S1 진행 금지)은 계속 유효하다.

- 상황: Figma URL이 플레이스홀더(`여기에_피그마_URL`)여서 S0을 수행할 수 없었다.
- 선택: S0~S1과 이에 의존하는 S3/S4/S5를 `[!]`로 표시하고 BLOCKERS.md에 기록했다. 의존성이 없는 S2만 진행했다.
- 이유: S1 커밋 이후 V1__init.sql은 수정할 수 없다. 화면 근거 없이 만든 도메인/스키마가 V1에 고정되는 것보다
  진행을 멈추는 쪽이 더 보수적이다.

## D2. S2는 기존 구현을 유지하고 테스트만 추가한다 (2026-11-09)

- 상황: `global.response.ApiResponse`, `global.exception.{ErrorCode, BusinessException, GlobalExceptionHandler}`는
  이미 develop에 머지되어 있었고(085be55) 테스트는 없었다.
- 선택: 구현은 그대로 두고 단위 테스트(`ApiResponseTest`, `BusinessExceptionTest`)와 핸들러 테스트
  (`GlobalExceptionHandlerTest`)를 추가했다.
- 핸들러 테스트는 `@WebMvcTest` 대신 `MockMvcBuilders.standaloneSetup` + 테스트 전용 스텁 컨트롤러를 썼다.
  아직 도메인 컨트롤러가 없고, `@WebMvcTest`는 OAuth2 Resource Server 자동 설정까지 올라와 핸들러 동작과 무관한
  보안 설정이 끼어들기 때문이다. 스텁 컨트롤러는 테스트 소스에 있으므로 ArchUnit 검사 대상(`DO_NOT_INCLUDE_TESTS`)이 아니다.

## D3. Figma는 지정 노드(6:3)가 아니라 GUI 페이지(13:1700)를 근거로 삼는다 (2026-11-09)

- 상황: URL의 `node-id=6-3`은 "Form factors & Display" 페이지로, IVI 디자인 키트의 폼팩터 템플릿뿐이고 서비스 화면이 없다.
- 선택: 같은 파일의 `GUI` 페이지에 있는 서비스 흐름 ①~⑤를 `figma-spec.md`에 정리했다. `이전`(이전 시안),
  `Foundation`, `Components`는 제외했다.
- 이유: 6:3만으로는 데이터 항목을 하나도 도출할 수 없다. 같은 파일 안에서 실제 서비스 화면이 명확히 구분되어 있으므로 추측이 아니다.

## D4. 도메인(바운디드 컨텍스트) 경계 (2026-11-09)

`figma-spec.md`의 화면에서 저장이 필요한 데이터만 도메인으로 잡았다.

| 도메인 패키지 | Aggregate Root (테이블) | 책임 |
|---|---|---|
| `user` | `User` (`users`) | 외부 Issuer JWT `sub` ↔ 내부 `Long` ID 매핑, 폰 푸시 토큰 |
| `place` | `SavedPlace` (`saved_places`) | ⑤ 빠른 목적지(집/회사) |
| `parkinglot` | `ParkingLot` (`parking_lots`), `OccupancyForecast` (`parking_lot_occupancy_forecasts`) | 주차장 기본 정보·요금·무료 시간, ① 시간대별 예측 만차율 |
| `arrival` | `ArrivalSession` (`arrival_sessions`) | ①~③ 도착 모드: 목적지, 목적지 주차장, 예상 도착 시각, 선택한 경유 주차장, 상태 |
| `parking` | `ParkingRecord` (`parking_records`) | ④⑤ P단 주차 위치(GPS), 층·구역 메모, 폰 전송, 출차 |

- ② 추천 후보는 저장하지 않는다. 주차장 위치·예측으로 조회 시점에 계산할 수 있고, 저장 근거(이력 화면 등)가 없다.
  사용자의 선택 결과만 `arrival_sessions.waypoint_parking_lot_id`로 남긴다.
- 혼잡도 등급(여유/혼잡)은 만차율에서 계산하는 값이라 컬럼으로 두지 않는다.
- 소요 시간/거리(18분 · 12.4km 등)는 내비 계산값이라 저장하지 않는다. `expected_arrival_at`만 클라이언트가 보낸다.
- 로그인/회원가입은 외부 Issuer 담당(CLAUDE.md)이라 `users`는 최초 요청 시 `sub`로 생성하는 최소 테이블이다.

## D5. 도메인 사이는 FK 없이 ID 컬럼 + 인덱스, 도메인 내부는 FK (2026-11-09)

- 상황: CODE_CONVENTIONS 3장 서문은 "DB 참조는 FK"라고 하고, 야간 지시서는 "도메인 사이는 FK 대신 ID 컬럼 + 인덱스(7.3)"라고 한다.
- 선택: 도메인 사이 참조(`user_id`, `parking_lot_id` 등)는 FK 없이 `BIGINT` + 인덱스. 같은 도메인 내부
  (`parking_lot_occupancy_forecasts.parking_lot_id` → `parking_lots`)만 FK를 둔다.
- 이유: 지시서가 이 작업의 명시 지시이고, 도메인 경계를 DB 결합으로 묶지 않는 쪽이 7.3/7.8(분리 가능성)과 맞는다.
  컨벤션 문서 자체는 범위 밖이라 수정하지 않았다.

## D6. 위치 컬럼은 `geometry(Point, 4326)` (2026-11-09)

- 선택: 모든 위치는 `geometry(Point, 4326)` + GiST 인덱스. 엔티티 타입은 JTS `Point`(hibernate-spatial 기본 매핑).
- 이유: hibernate-spatial의 기본 매핑이 geometry라 `ddl-auto: validate`와 충돌이 없다. 미터 단위 거리 계산은 쿼리에서
  `::geography` 캐스팅으로 처리한다.

## D7. 공통 엔티티·Clock 빈 추가 (2026-11-09)

- `global.entity.BaseCreatedEntity`(id, createdAt — 컨벤션 3장에 명시된 이름)와 이를 상속한 `BaseTimeEntity`(updatedAt)를 추가했다.
  시각은 JPA Auditing 설정을 새로 켜지 않도록 Hibernate `@CreationTimestamp`/`@UpdateTimestamp`로 채운다.
- `global.config.ClockConfig`로 `Clock`(UTC) 빈을 등록했다. 주차 경과 시간, 세션 종료 시각 같은 규칙을 엔티티 메서드에
  `Instant`로 넘겨 단위 테스트에서 고정하기 위해서다. 설정 파일(`application.yml`) 변경은 없다.
- `UserWriteService.resolveUserId`는 같은 subject의 최초 요청이 동시에 들어오면 유니크 제약 위반으로 한쪽이 실패할 수 있다.
  재시도하면 성공하는 드문 경우라 지금은 별도 처리를 두지 않는다(5.4).

## D8. parkinglot 도메인 세부 (2026-11-09)

- 좌표 값 객체 `global.geo.GeoPoint`(위도/경도, SRID 4326)를 여러 도메인이 공유한다. 엔티티는 JTS `Point`를 저장하고
  getter는 `GeoPoint`를 돌려준다. 엔티티 공통 검증은 `global.exception.DomainPreconditions`로 모았다.
- 혼잡도 등급 임계값: 70% 미만 AVAILABLE(여유), 90% 미만 NORMAL, 이상 CONGESTED(혼잡). 화면 근거는 "95% → 혼잡 예상"
  하나뿐이라 임계값은 가정이다. `CongestionLevel` 한 곳에서만 바꾸면 된다.
- 응답 DTO의 혼잡도는 enum이 아니라 문자열(`name()`)이다. 다른 도메인(arrival)이 `parkinglot.entity`를 참조하지 않게 하기 위해서다(7.6).
- 예측 슬롯: 기준 시각 `at`의 예측 = `(at - 30분, at]` 구간에서 가장 늦은 예측. 화면의 30분 간격 막대에 맞췄다.
- 주변 검색: 반경 1~5000m, 최대 10개. 미터 거리는 `::geography` 캐스팅으로 계산한다.
- 주차장 등록/예측 기록용 Write 서비스는 만들었지만 Figma에 관리자 화면이 없으므로 S4에서 공개 API로 노출하지 않는다
  (데이터 적재 경로는 미정 — 외부 배치/관리 도구가 정해지면 연결).

## D9. arrival 도메인 세부 (2026-11-09)

- 새 목적지로 도착 모드를 시작하면 기존 ACTIVE 세션을 CANCELLED로 끝내고 새로 만든다(내비에서 목적지를 바꾸는 흐름).
  Hibernate가 INSERT를 UPDATE보다 먼저 flush하므로 취소 후 명시적으로 `flush()`한다.
- 대안 주차장 검색 반경 500m(화면의 "300m 옆 공영주차장" 포함), ① 막대그래프 범위는 도착 시각 ±1시간(화면 13:30~15:00).
- 추천 규칙(`ParkingRecommendationPolicy`): 목적지 주차장 예측보다 만차율이 낮은 주차장, 목적지 예측이 없으면 혼잡이 아닌 주차장.
  예측이 없는 주차장은 대안에서 뺀다. 만차율·거리 순 첫 번째가 추천. ①의 "더 여유 있는 주차장 N곳" = 대안 수.
- 목적지 주차장은 경유지로 선택할 수 없다(의미 없는 경로 변경). 경유지 해제(되돌리기)는 화면 근거가 불확실해 만들지 않았다.
- 다른 사용자의 세션/장소 접근은 존재 여부를 숨기려고 FORBIDDEN 대신 NOT_FOUND로 응답한다(place도 동일).

## D10. parking 도메인 세부 (2026-11-09)

- 주차 시각은 클라이언트 값이 아니라 서버 `Clock` 기준이다(P단 이벤트 수신 시각).
- 출차하지 않은 기록이 있는 상태에서 다시 P단 주차가 들어오면 이전 기록을 지금 시각으로 출차 처리하고 새로 기록한다
  (출차 신호 누락 대비). arrival과 같은 이유로 취소 후 `flush()`.
- ⑤ 요약은 출차하지 않은 기록 기준이다. 남은 무료 시간 = max(0, 주차장 무료 시간 − 경과). 주차장을 모르면 무료 시간 정보는 null.
- **폰 전송(`sent_to_phone_at`)은 구현하지 않았다.** FCM 설정(`FIREBASE_CREDENTIALS_PATH`)과 Firebase 빈 구성이 없고,
  인증 흐름(CLAUDE.md TBD)도 정해지지 않아 푸시 발송을 추측으로 만들지 않았다. 컬럼은 V1에 남아 있으며 연동 시 엔티티 메서드
  (`markSentToPhone`)와 `@TransactionalEventListener(AFTER_COMMIT)` 리스너를 추가하면 된다. `User.pushToken` 등록 API는 S4에서 제공한다.
- P단 주차 시 진행 중인 도착 모드를 자동 완료하는 연동은 화면 근거가 없어 넣지 않았다. 클라이언트가 arrival 완료 API를 호출한다.
