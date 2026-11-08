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
