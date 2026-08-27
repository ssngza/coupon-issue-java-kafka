# Core Beliefs & Engineering Invariants

## 1. Core Invariants (절대 불변 원칙)

1. **No Direct DB Stock Decrement**: 선착순 검증 단계에서 MySQL DB를 직접 조회/차감하지 않는다. 모든 재고 검증은 Redis에서 원자적으로 끝낸다.
2. **Strict Idempotency**: 1인 1쿠폰 발급 정책은 Redis 레벨과 DB Unique Constraint 레벨의 2중 방어로 멱등성을 보장한다.
3. **Guaranteed Fallout Compensation**: Kafka Consumer에서 DB 영속화 실패 시, 유실된 재고는 DLT를 통해 반드시 Redis로 원상 복구(Restitution)되어야 한다.

## 2. Cost-Efficiency Invariants

- 단일 EC2 (`t3.small` / `t3.medium`) 내 Docker Compose 환경에서 모든 인프라(Nginx, App, Redis, Kafka)를 컨테이너화하여 월 비용을 극소화한다.
