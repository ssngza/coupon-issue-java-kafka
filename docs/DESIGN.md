# System Design & Implementation Details

## 1. Redis Lua Scripting Strategy

- 파일 위치: `src/main/resources/scripts/issue_coupon.lua`
- `KEYS[1]`: `coupon:{couponId}:stock` (잔여 수량 Key)
- `KEYS[2]`: `coupon:{couponId}:users` (발급받은 유저 ID Set Key)
- `KEYS[3]`: `coupon:{couponId}:user:{userId}:issue-status` (발급 최종 상태 Key)
- `ARGV[1]`: `userId`
- `ARGV[2]`: 상태 TTL 초 (`600`)

`SISMEMBER` -> `GET stock` -> `DECR` -> `SADD` -> `SET PENDING EX 600` 순서는 하나의 Lua 실행에서 완료한다. MySQL은 이 선착순 검증과 재고 차감 경로에 참여하지 않는다.

## 2. Kafka Topic Strategy

- `coupon.issue.request`: 쿠폰 발급 성공 이벤트 스트림 (Partitions: 3)
- `coupon.issue.retry`: 최초 영속화 실패 후 1회 재시도
- `coupon.issue.dlt`: 최종 영속화 실패 메시지 격리

발급 성공 이벤트의 payload는 `couponId`, `userId`, `issuedAt`으로 구성한다. Consumer 영속화 실패는 `@RetryableTopic(attempts = "2")`으로 최초 처리 후 1회 재시도하며, DLT Consumer는 `INCR`과 `SREM`으로 Redis 상태를 보상한다.

Spring Kafka의 기본 자동 접미사 이름을 사용하지 않는다. `coupon.issue.retry`와 `coupon.issue.dlt`를 사용하도록 토픽 이름 설정을 명시적으로 구성한다. Kafka 발행 실패는 `INCR`, `SREM`, `SET FAILED EX 600`으로 보상하고, Consumer 성공은 `SET SUCCESS EX 600`으로 상태를 완료한다.

## 3. 최종 응답 상태 전략

- 컨트롤러는 Redis 상태 키를 최대 5초 동안 대기한다.
- 최종 상태가 `SUCCESS`면 발급 완료를, `FAILED`면 보상 완료된 발급 실패를 반환한다.
- 5초 안에 상태가 결정되지 않으면 `PENDING`을 반환한다.
- 클라이언트는 `GET /api/coupons/{couponId}/issues/{userId}/status`를 폴링해 `PENDING`의 최종 결과를 확인한다.
- DLT 레코드는 실패 원본으로 보존하고 구조화 로그에는 쿠폰, 사용자, 실패 단계, 예외, 보상 결과를 기록한다.
