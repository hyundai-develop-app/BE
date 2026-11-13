# 역할별 모델 배정

2026-11-12 사용자 결정. 작업마다 자동 선택하지 않고 아래 기본값을 사용한다.
이 문서는 향후 공통 스킬로 옮길 때 참고할 결정 기록이며, 실제 모델 실행은 각 설정 파일이 담당한다.

| 역할 | 도구·모델 | 실제 설정 위치 |
|---|---|---|
| 관리자 | Claude Code · Claude Fable 5.2 | `.claude/settings.json`의 `model` |
| 테스트 작성자 | Codex CLI · GPT-6.1 Sol · high | `scripts/codex-models.json`의 `test_writer` |
| 구현자(writer) | Codex CLI · GPT-6.1 Sol · high | `scripts/codex-models.json`의 `writer` |
| 리뷰어 | Claude Code `verifier` · Claude Opus 5.5 | `.claude/agents/verifier.md`의 `model` |

## 결정 이유와 역할 분리

- 관리자는 요구사항 정리, 명세, 작업 분할과 통합 판단을 맡으므로 Fable을 사용한다.
- 테스트 작성자와 구현자는 Sol을 사용하되 서로 다른 Codex 세션으로 실행한다.
- 리뷰는 구현 세션과 독립된 Claude Opus가 맡아 원래 명세·테스트·변경 코드를 검토한다.
- 기존 Claude `verifier`의 write-tests 모드는 Codex 테스트 작성자로 이전했다. `verifier`는 리뷰만 담당한다.
- 역할 이름은 권한과 책임을 뜻하며 모델 이름과는 별개다. 모델을 바꿔도 역할별 수정 범위와 검증 게이트는 유지한다.

## 실행 방법

1. 이 저장소에서 Claude Code 관리자를 시작한다. 프로젝트 기본 모델은 Fable 5.2다.
2. 관리자가 명세와 독립 worktree를 준비하고 작업을 등록한다.
3. `python3 scripts/pipeline.py run-tests <작업 ID>`로 테스트 작성자를 실행한다.
4. 관리자가 테스트를 실행하여 Red 원인을 확인하고 기존 `tests-ready` 게이트를 수행한다.
5. `scripts/run-writer.sh <작업 ID>`로 별도 구현자를 실행한다.
6. 관리자가 Claude `verifier` 리뷰어를 호출한다. 작업 worktree 절대 경로, 명세, diff, snapshot과 테스트 결과를 전달한다.
7. 기존 `review`, `integrate` 게이트로 검사하고 통합한다.

상세 절차는 `docs/AGENT_WORKFLOW.md`, 테스트 작성 지침은 `docs/TEST_WRITER.md`를 따른다.
파이프라인은 Codex 모델을 `-m`과 추론 설정으로 명시한다. Claude 관리자·리뷰어 실행은 관리자의 세션에서 진행하며 스크립트가 자동으로 시작하지 않는다.

## 모델 변경과 스킬로 옮길 때

- 모델 변경은 사용자가 정한다. 관리자는 작업 특성에 따라 변경을 제안할 수 있지만 임의로 다른 모델로 대체하지 않는다.
- 실제 설정 파일과 이 표를 함께 수정한다. 고정된 버전은 자동으로 최신 버전으로 바뀌지 않는다.
- Claude의 세션 내 모델 선택, 명령행·로컬·조직 설정이 프로젝트 기본값보다 우선할 수 있다. 관리자 시작 시 `/model`로 실제 선택을 확인한다.
- 이 설정은 현재 Codex 대화의 모델을 변경하지 않는다. 새로 실행되는 각 도구에 적용한다.
- 접근 권한이나 인증 오류는 로그로 보고하고 해결한다. 모델 변경으로 우회하지 않는다.
- 향후 스킬에는 역할·순서·권한·검증 절차를 담고, 프로젝트별 모델 이름과 실행 설정은 분리한다.
- 기존 3회 반려 한도와 Red/리뷰/통합 검증은 모델 배정과 무관하게 유지한다.

모델 ID 확인: [Anthropic 모델 안내](https://platform.claude.com/docs/en/models/overview),
[OpenAI 모델 선택 안내](https://developers.openai.com/api/docs/guides/model-selection).
