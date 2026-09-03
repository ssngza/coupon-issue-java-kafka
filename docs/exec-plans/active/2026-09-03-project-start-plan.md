# Project Start Plan

작성일: 2026-09-03
상태: Active

## 목표

쿠폰 발급 시스템의 초기 제작을 시작하기 위한 작업 순서를 정리하고, 구현과 검증을 단계적으로 진행한다.

## Todo

- [ ] 저장소 구조와 실행 환경을 최종 확인한다.
- [ ] Docker Compose 기반 로컬 기동 경로를 정리하고, Nginx / App / Redis / Kafka / MySQL 구성 상태를 점검한다.
- [ ] Redis Lua Script 기반 발급 원자 처리와 관련 검증 테스트를 준비한다.
- [ ] Kafka Producer / Consumer 비동기 영속화 흐름과 최종 상태 키 처리를 준비한다.
- [ ] DLT 및 fallout 보상 흐름, 재시도 정책, 복구 테스트를 준비한다.
- [ ] 동시성 및 정합성 검증용 통합 테스트와 부하 테스트 기준을 정리한다.

## 기준 문서

- [ARCHITECTURE.md](../../ARCHITECTURE.md)
- [HARNESS_SPEC.md](../../HARNESS_SPEC.md)
- [docs/design-docs/core-beliefs.md](../../docs/design-docs/core-beliefs.md)
- [docs/PLANS.md](../../docs/PLANS.md)

