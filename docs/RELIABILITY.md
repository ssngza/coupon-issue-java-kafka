# System Reliability & Fault Tolerance

## 1. 장애 시나리오 및 대응

1. **Redis OOM 방지**: `maxmemory 256mb` 및 `allkeys-lru` 정책 설정.
2. **Kafka Consumer Deadlock**: `@RetryableTopic(attempts = "2")`으로 최초 처리 후 1회 재시도하고, 실패 시 `coupon.issue.dlt`로 안전하게 우회.
3. **Kafka 발행 실패**: Producer가 Redis 재고 `INCR`, 사용자 발급 이력 `SREM`, 상태 `FAILED`를 기록해 발행되지 않은 발급을 즉시 보상.
4. **영속화 최종 실패**: DLT Consumer가 Redis 재고를 `INCR`, 사용자 발급 이력을 `SREM`, 상태를 `FAILED`로 기록하여 재고 유실을 방지. DLT 레코드와 구조화 로그에 실패 원인을 보존.
5. **응답 대기 시간 초과**: 컨트롤러는 5초 이후 `PENDING`을 반환하고 상태 조회 API가 최종 결과를 제공.
6. **EC2 OOM Killer 대응**: 2GB Swap Memory 활성화로 JVM/Kafka 메모리 스파이크 완충.
