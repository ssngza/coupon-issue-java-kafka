# Quality Score & Testing Strategy

## 1. 테스트 하네스 기준

- **Unit & Integration Test**: JUnit5, AssertJ, Mockito
- **동시성 테스트**: `RedisConcurrencyTest.java` (ExecutorService 기반 멀티스레드 1,000건 동시 요청)
- **통합 검증**: `KafkaFalloutIntegrationTest.java` (DB 오류 시뮬레이션 후 Redis 재고 복구 검증)

## 2. 완료 조건 (Definition of Done)

- `./gradlew test` 전체 통과
- 재고 100개 기준 1,000건 동시 요청 시 정확히 100건만 성공하고 초과 발급(Over-issue) 0건 확인
