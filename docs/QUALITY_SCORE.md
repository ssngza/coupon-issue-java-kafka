# Quality Score & Testing Strategy

## 1. 테스트 하네스 기준

- **Unit & Integration Test**: JUnit5, AssertJ, Mockito
- **동시성 테스트**: `RedisConcurrencyTest.java` (ExecutorService 기반 멀티스레드 1,000건 동시 요청)
- **이벤트 영속화 테스트**: `CouponConsumerIntegrationTest.java` (Embedded Kafka 또는 Testcontainers로 이벤트 발행 후 MySQL 적재 검증)
- **통합 검증**: `KafkaFalloutIntegrationTest.java` (DB 오류 시뮬레이션 후 Redis 재고 복구 검증)

## 2. 완료 조건 (Definition of Done)

- `./gradlew test` 전체 통과
- 재고 100개 기준 1,000건 동시 요청 시 정확히 100건만 성공하고 초과 발급(Over-issue) 0건 확인
- DLT 최종 실패 후 Redis 재고 `INCR` 및 발급 사용자 Set `SREM` 복구 확인
