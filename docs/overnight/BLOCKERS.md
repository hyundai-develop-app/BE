# 막힌 항목

## S0 Figma 덤프 — Figma URL 없음 (2026-11-09)

- 프롬프트로 전달된 값이 `Figma URL: 여기에_피그마_URL` 플레이스홀더라 fileKey를 추출할 수 없다.
- `gradlew check` 실패가 아니라 입력 부재이므로 재시도해도 해결되지 않는다.
- **해결 방법**: 실제 URL로 다시 실행한다. `FIGMA_URL='https://www.figma.com/design/<fileKey>/...' ./scripts/overnight.sh`
  그 다음 PROGRESS.md의 S0~S5 `[!]`를 `[ ]`로 되돌린다.

## S1, S3, S4, S5 — S0에 의존

- S1(ERD)은 `figma-spec.md`를 근거로만 작성하도록 되어 있다. 근거 없이 스키마를 지어내면 S1 커밋 후 V1이
  수정 금지가 되어 잘못된 스키마가 고정되므로 진행하지 않았다 (DECISIONS.md D1).
- S3/S4는 S1에서 도메인 목록이 정해져야 펼칠 수 있다. S5(DONE)는 전체가 끝나지 않았으므로 만들지 않았다.
