# 진행 상황 (자율 작업이 갱신함 — 체크박스 `[x]` 완료, `[!]` 막힘은 BLOCKERS.md 참고)

- [x] S0 Figma 덤프 → docs/overnight/figma-spec.md
- [x] S1 ERD(DDL) → V1__init.sql, 도메인 목록 확정 후 아래 S3/S4를 도메인별로 펼칠 것
- [x] S2 공통 응답/예외 (global)
- S3 도메인 구현 (도메인 경계: DECISIONS.md D4)
  - [x] S3-user (`users`)
  - [x] S3-parkinglot (`parking_lots`, `parking_lot_occupancy_forecasts`)
  - [x] S3-place (`saved_places`)
  - [ ] S3-arrival (`arrival_sessions`)
  - [ ] S3-parking (`parking_records`)
- S4 Controller/API
  - [ ] S4-user
  - [ ] S4-parkinglot
  - [ ] S4-place
  - [ ] S4-arrival
  - [ ] S4-parking
- [ ] S5 전체 check 통과 및 DONE 파일 생성
