# Engineering Roadmap & Milestones

- [x] **Milestone 1**: AWS 비용 최적화 인프라 및 Docker Compose 환경 구성. 앱 이미지와 RDS 연결값을 확정하고 기동 검증이 완료될 때까지 진행 중으로 유지
- [x] **Milestone 2**: Redis Lua Script 기반 원자적 수량 제어 및 `RedisConcurrencyTest` 작성
- [x] **Milestone 3**: `CouponIssueEvent` 기반 Kafka Producer/Consumer 비동기 영속화, Redis 최종 상태 키, 5초 응답 대기와 Consumer 멱등성 테스트 구축
- [x] **Milestone 4**: `@RetryableTopic` 및 `@DltHandler` 기반 `INCR`·`SREM` 폴아웃 보상, Kafka 발행 실패 보상, DLT 보상 단위 테스트 구현
- [x] **Milestone 5**: Python 1,000 동시 요청 부하 테스트 및 Redis-MySQL 정합성 검증 기준 작성

세부 완료 기준은 [HARNESS_SPEC.md](../HARNESS_SPEC.md)를 따른다.
