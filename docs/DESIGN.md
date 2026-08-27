# System Design & Implementation Details

## 1. Redis Lua Scripting Strategy

- 파일 위치: `src/main/resources/scripts/issue_coupon.lua`
- `KEYS[1]`: `coupon:{id}:stock` (잔여 수량 Key)
- `KEYS[2]`: `coupon:{id}:users` (발급받은 유저 ID Set Key)
- `ARGV[1]`: `userId`

## 2. Kafka Topic Strategy

- `coupon.issue.request`: 쿠폰 발급 성공 이벤트 스트림 (Partitions: 3)
- `coupon.issue.retry`: 일시적 예외 발생 시 지수 백오프 재시도
- `coupon.issue.dlt`: 최종 영속화 실패 메시지 격리
