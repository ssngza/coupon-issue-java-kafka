# AI Agent Instructions & Operating Rules

본 프로젝트는 Codex, Claude 등 AI 코딩 에이전트와 하네스 주도 개발(Harness-Driven Development) 방식으로 협업합니다.

## 1. 에이전트 작업 원칙

1. **Rule of No Assumptions**: 아키텍처나 비즈니스 규칙이 모호할 경우 임의로 코드를 작성하지 않고 명세를 먼저 확인합니다.
2. **Invariant Priority**: `ARCHITECTURE.md` 및 `docs/design-docs/core-beliefs.md`의 핵심 불변 규칙을 위반하는 코드는 절대 작성하지 않습니다.
3. **Test-First Execution**: 모든 기능 변경/추가 시 반드시 `src/test/` 디렉터리에 대응하는 검증 테스트(JUnit5 / Testcontainers) 코드를 함께 작성합니다.

## 2. 작업 순서

1. `docs/exec-plans/active/`에 활성화된 실행 계획 확인
2. 변경 대상 코드 작성 및 수정
3. 단위/통합 테스트 실행 (`./gradlew test`)
4. 완료 후 `docs/exec-plans/completed/`로 이동 및 문서 업데이트
