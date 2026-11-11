# 음성(STT) 입력 · 폰 푸시 · 차량 · 저장 장소 타입 설계

- 이슈: #5
- 브랜치: `feat/5-voice-push-vehicle` (`develop` 9e9ee8e 기준)
- 상태: 사용자 검토 대기

## 1. 목적과 범위

피그마 GUI 흐름(① 대기 → ② 브리핑 → ③ 경유지 → ④ P단 주차 기록 → ⑤ 다음 시동 요약)의 기본 API는 develop에 이미 있다
(`arrival`, `parking`, `parkinglot`, `place`, `user`, 목록은 `docs/overnight/API.md`). 이 작업은 그 위에 빠진 부분만 더한다.

| # | 추가할 것 | 화면 근거 |
|---|---|---|
| 1 | 음성 입력: 교체 가능한 STT 포트 + 발화 해석기 + 음성 엔드포인트 2개 | ② "응" 한마디로 경유지, ④ "지하 2층 C구역" |
| 2 | 폰 푸시 전송 | ④ "위치와 함께 폰으로 보냈어요" |
| 3 | 차량 도메인(사용자가 직접 등록) | 사용자 요청 |
| 4 | 저장 장소 타입(HOME/WORK/OTHER)과 수정 API | ⑤ "집", "회사" |

### 합의된 전제

- 주차장·만차율 예측·요금 데이터는 이미 DB에 있다고 보고 처리한다(외부 연동 없음).
- STT는 **서버가 처리**한다. 기본 공급자는 **현대차 제공 API**이고, 설정만으로 공급자를 바꾸거나 추가할 수 있어야 한다.
- 차량·집·회사 정보는 사용자가 직접 입력한다. 차량과 휴대폰은 **같은 사용자 계정**(JWT)으로 로그인한다.
- 브리핑 시점은 클라이언트(IVI)가 ETA로 판단한다(기존 `GET /arrivals/{id}/briefing` 그대로).
- 기존 API의 경로·이름·응답 필드는 바꾸지 않는다. 필드 **추가**만 한다.

### 범위 밖

- 인증 방식 변경. 기존 JWT subject → 사용자 자동 생성 방식(DECISIONS D11)을 그대로 쓴다.
- `SecurityFilterChain`(BLOCKERS.md), 서버의 주행 상태 추적, 경로 계산, 다중 공급자 Failover(컨벤션 3.7).
- 차량 단위의 "진행 중 기록 하나" 제약. 지금은 사용자 단위 제약(`uk_*_user_active`)을 유지한다(7장 참고).

## 2. 구조

```
com.hmc.hmcbe
├── global
│   ├── voice     SpeechToText 포트, VoiceInputResolver, 공급자 구현(impl/)        ← 신규
│   └── push      PushSender 포트, 구현(impl/)                                   ← 신규
├── vehicle       차량 등록·조회·수정·삭제                                        ← 신규 도메인
├── arrival       + voice/BriefingReplyInterpreter, 브리핑 음성 응답 API
├── parking       + voice/ParkingSpotInterpreter, 층·구역 음성 API, 푸시 이벤트 리스너
├── place         + type(HOME/WORK/OTHER), 수정 API
└── user          + 푸시 토큰 조회 메서드(UserReadService)
```

- 도메인 사이 호출은 서비스 인터페이스 또는 이벤트로만 한다(컨벤션 7.6, ArchUnit).
- `global.voice`, `global.push`는 어떤 도메인에도 의존하지 않는다.

### 교체 지점

| 포트 | 위치 | 구현체 | 선택 설정(기본값) |
|---|---|---|---|
| `SpeechToText` | `global.voice` | `HyundaiSpeechToText`, `FakeSpeechToText` | `hmc.voice.stt.provider` (`fake`) |
| `BriefingReplyInterpreter` | `arrival.voice` | `RuleBasedBriefingReplyInterpreter` | `hmc.voice.interpreter.briefing-reply` (`rule`) |
| `ParkingSpotInterpreter` | `parking.voice` | `RuleBasedParkingSpotInterpreter` | `hmc.voice.interpreter.parking-spot` (`rule`) |
| `PushSender` | `global.push` | `FcmPushSender`, `LoggingPushSender` | `hmc.push.provider` (`log`) |

- 구현체는 `@ConditionalOnProperty(name = ..., havingValue = "<이름>")`로 등록하고, 기본 구현체에만 `matchIfMissing = true`를 둔다.
  값이 어느 구현체와도 맞지 않으면 빈이 없어서 기동이 실패한다. 오타를 운영 중이 아니라 기동 시점에 잡기 위해서다.
- **공급자 추가 절차**: `impl`에 구현 클래스 추가 → `@ConditionalOnProperty` 값 지정 → 필요한 설정을 `VoiceProperties`
  (또는 `PushProperties`)의 하위 record로 추가 → 공유 계약 테스트를 상속한 테스트 추가. 서비스 코드는 고치지 않는다.
- STT(음성→텍스트)와 해석(텍스트→의도)을 나눠서 "현대차 STT + 규칙 해석", "현대차 STT + LLM 해석"처럼 따로 조합할 수 있다.

## 3. 음성 입력

### 3.1 `global.voice`

```java
public interface SpeechToText {
    Transcript transcribe(AudioClip clip);          // 실패 시 SpeechToTextException
}
public record AudioClip(byte[] bytes, String contentType, Locale locale) {}
public record Transcript(String text) {}           // 빈 문자열 가능(무음)
public record Utterance(String text, UtteranceSource source) {}   // source: AUDIO | TEXT
```

- `VoiceInputResolver`(`@Component`): 컨트롤러가 받은 `MultipartFile audio`와 `String text` 중 **정확히 하나**를 받아 `Utterance`로 만든다.
  - 둘 다 없거나 둘 다 있음 → `VOICE4001 INVALID_VOICE_INPUT`
  - `audio` 크기 > `hmc.voice.max-audio-bytes`(기본 2MB) → `VOICE4002 AUDIO_TOO_LARGE`
  - `audio` Content-Type이 `hmc.voice.allowed-content-types`(기본 `audio/wav`, `audio/x-wav`, `audio/mpeg`, `audio/mp4`, `audio/ogg`, `audio/webm`)에 없음 → `VOICE4003 UNSUPPORTED_AUDIO_FORMAT`
  - `text`는 앞뒤 공백을 제거하고 200자를 넘으면 `VOICE4001`
  - 언어는 `ko-KR`로 고정한다(`hmc.voice.locale`).
- STT 호출은 **트랜잭션 밖**에서 한다(TECH_DECISIONS CON-05). 해석 이후의 상태 변경만 쓰기 서비스의 트랜잭션에서 처리한다.
- `SpeechToTextException`(`RuntimeException`): 공급자 장애, 시간 초과, 인증 실패. 전역 핸들러에서 `VOICE5021 STT_UNAVAILABLE`(HTTP 502)로 바꾼다.
  로그에는 공급자 이름과 원인만 남기고, 발화 원문과 오디오는 남기지 않는다(OPS-05).

### 3.2 공급자 구현

- `FakeSpeechToText`(`provider=fake`): 오디오 내용과 상관없이 `hmc.voice.stt.fake.transcript`(기본 `"응"`)를 돌려준다. 테스트와 시연용이다.
  시연에서 원하는 문장이 필요하면 `text` 필드로 보내면 된다.
- `HyundaiSpeechToText`(`provider=hyundai`): `RestClient`로 현대차 제공 STT API를 호출한다.
  설정은 `hmc.voice.stt.hyundai.base-url`, `api-key`, `connect-timeout`(기본 2s), `read-timeout`(기본 5s)이고,
  환경변수는 `HYUNDAI_STT_BASE_URL`, `HYUNDAI_STT_API_KEY`이다. `.env.example`에도 추가한다.
  **요청·응답 형식은 현대차 API 명세를 받은 뒤 확정한다(9장 미결 사항).** 명세가 오기 전까지 이 구현체는 만들지 않고,
  `provider=fake`로 개발·시연한다. 나머지 작업은 명세 없이 진행할 수 있다.

### 3.3 발화 해석기

| 해석기 | 입력 → 출력 | 규칙 기반 기본 구현 |
|---|---|---|
| `BriefingReplyInterpreter` | 텍스트 → `ReplyIntent { ACCEPT, DECLINE, UNKNOWN }` | 문장부호를 제거하고 공백으로 단어를 나눈 뒤 키워드와 맞춘다. ACCEPT: 응, 어, 네, 예, 그래, 좋아, 가자, 그쪽, 거기로, 오케이, ok. DECLINE: 아니, 아뇨, 됐어, 괜찮아, 싫어, 원래대로. 한 글자 키워드(응·어·네·예)는 발화 전체이거나 첫 단어일 때만 인정한다. 두 글자 이상은 단어 안에 포함되면 인정한다. 부정 키워드가 있으면 DECLINE이 우선이다("아니 괜찮아" → DECLINE, "응 아니" → DECLINE). 둘 다 없으면 UNKNOWN |
| `ParkingSpotInterpreter` | 텍스트 → `Optional<ParkingSpot(floor, zone)>` | 층: `지하\s*(\d+)\s*층`, `B\s*(\d+)`, `(\d+)\s*층` → `B2`/`3F` 형식. 구역: `([A-Za-z가-힣])\s*구역` 또는 알파벳 한 글자. 한글 알파벳 발음(에이, 비, 씨, 디, 이, 에프 …)은 영문으로 바꾼다. 층과 구역 중 **하나라도** 나오면 성공 |

- `ParkingSpot.toMemo()`: 사람이 읽는 메모를 만든다. `B2`+`C` → `"지하 2층 C구역"`, `3F` → `"3층"`. 이 값을 기존 `spot_memo`에 저장한다.
  층·구역용 컬럼은 새로 만들지 않는다. 해석 결과는 응답으로만 내려준다.
- 해석에 실패해도 오류가 아니다. HTTP 200에 `understood: false`를 담아 보내고 상태는 바꾸지 않는다. IVI가 다시 묻거나 카드 탭으로 대신한다.

### 3.4 엔드포인트

두 API 모두 `multipart/form-data`로 받는다. 파트는 `audio`(파일) **또는** `text`(문자열) 중 하나다.
STT를 나중에 차량 쪽으로 옮기면 클라이언트가 `text`만 보내면 되고, 서버 API는 바뀌지 않는다.

**② 브리핑 음성 응답**: `POST /api/v1/arrivals/{sessionId}/briefing/voice-reply`

| 파트 | 필수 | 설명 |
|---|---|---|
| `audio` / `text` | 둘 중 하나 | 발화 |
| `parkingLotId` | 예 | 브리핑에서 추천 카드로 보여 준 주차장 ID. 브리핑은 매번 계산하므로 클라이언트가 보여 준 값을 받는다 |

- ACCEPT → 기존 `ArrivalWriteService.selectWaypoint(userId, sessionId, parkingLotId)`를 그대로 호출한다(소유권, 끝난 세션 409, 주차장 존재 검증 포함).
- DECLINE → 상태를 바꾸지 않는다. UNKNOWN → `understood: false`.
- 응답 `BriefingVoiceReplyResponse { understood, intent, transcript, session: ArrivalSessionResponse }`
  - ③ 경유지 화면에 필요한 이름·거리는 기존처럼 브리핑 카드 값을 쓴다.
- 기존 탭 경로 `PUT /arrivals/{id}/waypoint`는 그대로 둔다.

**④ 층·구역 음성 입력**: `POST /api/v1/parking-records/{recordId}/spot/voice`

- 해석에 성공하면 기존 `updateSpotMemo` 흐름으로 `spot_memo = ParkingSpot.toMemo()`를 저장한다(출차 후면 409). 다시 말하면 덮어쓴다("틀렸다면 다시 말해주세요").
- 응답 `ParkingSpotVoiceResponse { understood, transcript, floor?, zone?, record: ParkingRecordResponse }`
- 기존 텍스트 경로 `PUT /parking-records/{id}/spot-memo`는 그대로 둔다(사람이 직접 고칠 때).

**처리 순서(두 API 공통)**

```
Controller → <Domain>VoiceService(트랜잭션 없음)
   1. VoiceInputResolver.resolve(audio, text)      ← audio면 SpeechToText 호출
   2. Interpreter.interpret(utterance.text())
   3. 해석 성공 시 기존 WriteService 메서드 호출      ← 여기서만 트랜잭션
   4. 응답 조립
```

- `ArrivalVoiceService`/`ParkingVoiceService` 인터페이스와 `impl`을 둔다(컨벤션 1.1, 1.2). 소유권 검증은 3단계의 기존 서비스가 한다.
  다만 STT 비용을 아끼려고 1단계 전에 Read 서비스로 소유권과 상태를 먼저 확인한다(없으면 404, 끝났으면 409).

## 4. 폰 푸시

### 4.1 `global.push`

```java
public interface PushSender {
    void send(PushMessage message);   // 실패 시 PushSendException
}
public record PushMessage(String token, String title, String body, Map<String, String> data, String collapseKey) {}
```

- `LoggingPushSender`(`provider=log`, 기본): 토큰 앞 8자와 제목만 INFO로 남긴다.
- `FcmPushSender`(`provider=fcm`): `firebase-admin`(이미 의존성 있음)의 `FirebaseMessaging`으로 보낸다.
  `FirebaseConfig` + `PushProperties`(컨벤션 3.6)에서 `firebase.credentials-path`로 `FirebaseApp`을 만든다. 이 빈들은 `provider=fcm`일 때만 등록한다.
  Android는 `collapseKey`, APNs는 `apns-collapse-id`로 같은 키를 넣어 알림 하나를 갱신한다.

### 4.2 전송 시점

| 이벤트(parking 도메인, 과거형) | 발행 위치 | 푸시 내용 |
|---|---|---|
| `ParkingRecordedEvent(recordId, userId)` | 주차 기록 생성 | "주차 위치를 저장했어요" / "{주차장 이름 또는 '현재 위치'} · {HH:mm} 주차" |
| `ParkingSpotUpdatedEvent(recordId, userId)` | `spot_memo` 저장(음성·텍스트 모두) | "주차 위치를 저장했어요" / "{주차장 이름} · {spot_memo}" |

- 두 알림 모두 `collapseKey = "parking-{recordId}"`를 쓴다. 휴대폰에는 알림 하나만 남고, 층·구역이 들어오면 내용이 갱신된다.
  대답이 없으면 첫 알림(GPS 위치만)이 그대로 남는다.
- `data`: `{ "type": "PARKING_RECORDED", "parkingRecordId": "..." }`. 앱은 이 값으로 `GET /parking-records/{id}`를 호출한다.
- 리스너 `parking/event/ParkingPushListener`: `@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Async`.
  1. `UserReadService.findPushToken(userId)`로 토큰을 조회하고, 없으면 끝낸다.
  2. `ParkingRecordReadService`로 표시 정보를 조회한다(주차장 이름은 `ParkingLotReadService`).
  3. `PushSender.send(...)`
  4. 성공하면 `ParkingRecordWriteService.markSentToPhone(recordId, now)`를 호출한다. 새 트랜잭션에서 `ParkingRecord.markSentToPhone(Instant)`가 `sent_to_phone_at`을 갱신한다(이미 있던 컬럼, D10).
  - 실패해도 주차 기록에는 영향을 주지 않는다. WARN 로그만 남기고 재시도하지 않는다.
- `@EnableAsync`는 `global/config/AsyncConfig`에 둔다. 테스트에서는 동기 실행기로 바꿔 검증한다.
- ④ 화면의 "위치와 함께 폰으로 보냈어요"는 클라이언트 문구다. API 응답은 전송 결과를 기다리지 않는다.

## 5. 차량(`vehicle` 도메인, 신규)

| 컬럼 | 타입 | 규칙 |
|---|---|---|
| `id` | BIGINT IDENTITY | |
| `user_id` | BIGINT NOT NULL FK users | |
| `name` | VARCHAR(50) NOT NULL | 예: "내 아이오닉". 빈 값 금지 |
| `plate_number` | VARCHAR(20) NULL | 공백 제거해 저장. 사용자 안에서 유니크(`uk_vehicles_user_plate`, NULL 제외) |
| `created_at`, `updated_at` | TIMESTAMPTZ | `BaseTimeEntity` |

| 메서드 | 경로 | 요청 | 응답 `data` |
|---|---|---|---|
| POST | `/api/v1/vehicles` | `{ name, plateNumber? }` | 201 `VehicleResponse { id, name, plateNumber? }` |
| GET | `/api/v1/vehicles` | — | `VehicleResponse[]`(등록 순) |
| PATCH | `/api/v1/vehicles/{vehicleId}` | `{ name?, plateNumber? }` | `VehicleResponse` |
| DELETE | `/api/v1/vehicles/{vehicleId}` | — | 없음 |

- 엔티티 `Vehicle.register(...)`, `update(...)`. 세터는 두지 않는다.
- `VehicleReadService.requireOwned(userId, vehicleId)`: 다른 도메인이 차량 ID를 검증할 때 쓴다.
- 기존 기록과 연결:
  - `parking_records.vehicle_id`, `arrival_sessions.vehicle_id`(NULL 허용, FK `ON DELETE SET NULL`)를 추가한다.
  - `ParkRequest`, `StartArrivalRequest`에 선택 필드 `vehicleId`를 추가한다. 값이 있으면 `requireOwned`로 검증한다(남의 차량이면 `VEHICLE4041`).
  - 응답 `ParkingRecordResponse`, `ArrivalSessionResponse`에 `vehicleId?`를 추가한다.
- 차량 삭제는 hard delete이다. 연결된 기록의 `vehicle_id`는 NULL이 된다.

## 6. 저장 장소 타입

- `saved_places.type VARCHAR(10) NOT NULL DEFAULT 'OTHER'`, `CHECK (type IN ('HOME','WORK','OTHER'))`를 추가한다.
  기존 행은 OTHER가 된다.
- HOME과 WORK는 사용자당 하나만 허용한다. 부분 유니크 인덱스 `uk_saved_places_user_type ON (user_id, type) WHERE type <> 'OTHER'`로 막는다.
  중복이면 `PLACE4092 DUPLICATE_PLACE_TYPE`(409)이다.
- `POST /saved-places` 요청에 `type?`(기본 OTHER)을 추가하고, `SavedPlaceResponse`에 `type`을 추가한다.
- 신규 `PATCH /api/v1/saved-places/{savedPlaceId}`: `{ name?, latitude?, longitude?, type? }`. 위도와 경도는 둘 다 보내거나 둘 다 생략한다(`COMMON4001`).
  엔티티 메서드는 `rename`, `relocate`, `changeType`이다.
- 정렬: `GET /saved-places`와 ⑤ 요약의 `quickDestinations`는 HOME → WORK → OTHER(생성 순) 순서로 정렬한다.

## 7. 스키마 마이그레이션

`V2__add_vehicles_and_saved_place_type.sql` 하나로 만든다. V1은 수정하지 않는다.

- `vehicles` 테이블, `idx_vehicles_user_id`, `uk_vehicles_user_plate`(`WHERE plate_number IS NOT NULL`)
- `parking_records.vehicle_id`, `arrival_sessions.vehicle_id`와 각 인덱스, FK
- `saved_places.type`, CHECK 제약, `uk_saved_places_user_type`

"진행 중 기록은 사용자당 하나" 제약은 그대로 둔다. 한 사용자가 차량 두 대를 동시에 주차하는 경우는 지원하지 않는다.
필요해지면 V3에서 `(vehicle_id) WHERE exited_at IS NULL`로 바꾸는 별도 작업으로 다룬다.

## 8. 오류 코드 (`docs/ERROR_CODES.md`에도 추가)

| 코드 | HTTP | 이름 | 언제 |
|---|---|---|---|
| VOICE4001 | 400 | INVALID_VOICE_INPUT | `audio`/`text`가 둘 다 없거나 둘 다 있음, `text` 200자 초과 |
| VOICE4002 | 400 | AUDIO_TOO_LARGE | 오디오 크기 초과 |
| VOICE4003 | 400 | UNSUPPORTED_AUDIO_FORMAT | 허용하지 않는 오디오 형식 |
| VOICE5021 | 502 | STT_UNAVAILABLE | STT 공급자 장애·시간 초과 |
| VEHICLE4001 | 400 | INVALID_VEHICLE | 차량 이름·번호판 형식 오류 |
| VEHICLE4041 | 404 | VEHICLE_NOT_FOUND | 차량 없음(남의 차량 포함) |
| VEHICLE4091 | 409 | DUPLICATE_PLATE_NUMBER | 같은 번호판이 이미 등록됨 |
| PLACE4092 | 409 | DUPLICATE_PLACE_TYPE | HOME/WORK가 이미 있음 |

- `VoiceErrorCode`는 `global/voice`에, `VehicleErrorCode`는 `vehicle/exception`에 둔다.
- multipart 파일 크기 상한(`spring.servlet.multipart.max-file-size`)은 `hmc.voice.max-audio-bytes`보다 조금 크게(3MB) 잡는다.
  서블릿 단계에서 막히는 `MaxUploadSizeExceededException`도 `VOICE4002`로 바꾼다.

## 9. 미결 사항

| 항목 | 막히는 작업 | 그 전까지 |
|---|---|---|
| **현대차 STT API 명세**(엔드포인트, 인증 방식, 오디오 포맷·샘플레이트, 응답 형식, 오류 형식, 호출 한도) | `HyundaiSpeechToText` 구현과 계약 테스트 | `provider=fake`로 개발·시연 |
| FCM 서비스 계정 키(`FIREBASE_CREDENTIALS_PATH`) | 실제 FCM 전송 확인 | `provider=log` |

## 10. 테스트

- **단위 테스트**
  - `RuleBasedBriefingReplyInterpreter`: 응, 네, 그래 가자, 아니, 아니 괜찮아, 응 아니, 빈 문자열, 관계없는 문장
  - `RuleBasedParkingSpotInterpreter`: 지하 2층 C구역, B2 씨 구역, 3층, 지하2층, C구역, 에이 구역, 의미 없는 문장
  - `ParkingSpot.toMemo`, `VoiceInputResolver`(오류 코드 3종), 엔티티 상태 전이(`Vehicle`, `SavedPlace.changeType`, `ParkingRecord.markSentToPhone`)
- **계약 테스트**: `SpeechToTextContractTest`(추상 클래스)를 공급자 구현마다 상속한다(컨벤션 4.5). Fake 구현에 먼저 적용한다.
  현대차 구현은 명세를 받은 뒤 MockRestServiceServer로 검증한다.
- **컨트롤러 테스트**: `@WebMvcTest` + `AuthFixture`. multipart(audio/text) 양쪽 경로, `understood: false`, 오류 코드를 확인한다.
- **통합 테스트**(Testcontainers PostGIS): 음성 응답 ACCEPT → `waypoint_parking_lot_id` 저장, 층·구역 음성 → `spot_memo` 저장 + 푸시 리스너 호출(테스트용 `PushSender` 기록) + `sent_to_phone_at` 갱신,
  V2 마이그레이션 제약(차량 번호판 유니크, HOME 중복 409, 차량 삭제 시 SET NULL).
- **ArchUnit**: 새 패키지(`vehicle`, `global.voice`, `global.push`)가 기존 규칙을 통과하는지 확인한다. `global.voice`와 `global.push`가 도메인 패키지에 의존하지 않는다는 규칙도 추가한다.

## 11. 문서 갱신

- `docs/overnight/API.md`: 새 엔드포인트와 추가 필드
- `docs/ERROR_CODES.md`: 8장의 오류 코드
- `docs/DATABASE.md`, `docs/erd/hmc-erd.sql`: V2 변경
- `CLAUDE.md`: Firebase는 FCM 푸시 용도로 확정. 음성 공급자 설정(`hmc.voice.*`)
- `.env.example`: `HYUNDAI_STT_BASE_URL`, `HYUNDAI_STT_API_KEY`
- `application.yml`: `hmc.voice.*`, `hmc.push.*`, multipart 상한

## 12. 작업 분할 (PR 단위)

| PR | 내용 | 의존 |
|---|---|---|
| A | `global.voice` 포트·Fake·`VoiceInputResolver`·해석기 2개·음성 엔드포인트 2개 + `global.push`·리스너·`markSentToPhone` | 없음 |
| B | `vehicle` 도메인 + V2 마이그레이션 + 저장 장소 타입·수정 API + 기존 요청/응답 필드 추가 | 없음 |
| (후속) | `HyundaiSpeechToText` | 현대차 API 명세 |

A와 B는 서로 다른 파일을 다루므로 병렬로 진행할 수 있다. 다만 `ParkingRecordResponse`는 A(푸시)는 읽기만 하고 B는 필드를 추가하므로,
B를 먼저 머지하고 A를 리베이스한다.
