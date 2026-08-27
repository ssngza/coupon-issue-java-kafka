# System Design & Implementation Details

## 1. Redis Lua Scripting Strategy

- 파일 위치: `src/main/resources/scripts/issue_coupon.lua`
- `KEYS[1]`: `coupon:{id}:stock` (잔여 수량 Key)
- `KEYS[2]`: `coupon:{id}:users` (발급받은 유저 ID Set Key)
- `ARGV[1]`: `userId`

`SISMEMBER` -> `GET stock` -> `DECR` -> `SADD` 순서는 하나의 Lua 실행에서 완료한다. MySQL은 이 선착순 검증과 재고 차감 경로에 참여하지 않는다.

## 2. Kafka Topic Strategy

- `coupon.issue.request`: 쿠폰 발급 성공 이벤트 스트림 (Partitions: 3)
- `coupon.issue.retry`: 일시적 예외 발생 시 지수 백오프 재시도
- `coupon.issue.dlt`: 최종 영속화 실패 메시지 격리

발급 성공 이벤트의 payload는 `couponId`, `userId`, `issuedAt`으로 구성한다. Consumer 영속화 실패는 `@RetryableTopic`으로 최대 3회 재시도하며, DLT Consumer는 `INCR`과 `SREM`으로 Redis 상태를 보상한다.
