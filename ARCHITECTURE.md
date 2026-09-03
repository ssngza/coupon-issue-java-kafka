# System Architecture

선착순 쿠폰 발급 및 폴아웃 처리 시스템의 전체 아키텍처 명세서입니다. 구현·검증 단계의 상세 기준은 [HARNESS_SPEC.md](HARNESS_SPEC.md)를 따른다.

## 1. 기술 구성

- **Application**: Java 17, Spring Boot 3.x 단일 애플리케이션. API Producer, Kafka Consumer, Fallout Worker를 함께 실행한다.
- **API Entrypoint**: Nginx. HTTP 80 포트에서 정적 테스트 UI를 제공하고 `/api/` 요청을 Spring Boot로 프록시한다. HTTPS/TLS는 이 프로젝트 범위에 포함하지 않는다.
- **Fast-path Tier**: Redis Atomic Counter, Set, 발급 상태 키. Lua Script로 재고 검증, 차감, 중복 발급 방지, `PENDING` 기록을 하나의 원자 연산으로 처리한다.
- **Event Streaming Tier**: Apache Kafka KRaft 단일 브로커. `coupon.issue.request`, `coupon.issue.retry`, `coupon.issue.dlt` 이벤트를 전달한다.
- **Persistence Tier**: AWS RDS for MySQL. `coupon`과 `coupon_history`를 최종 영속 저장소로 사용한다.
- **Quality Harness**: JUnit 5, AssertJ, Mockito, Testcontainers, JMeter로 동시성·비동기 영속화·폴아웃 복구를 검증한다.

## 2. Redis 원자 처리

- 재고 키: `coupon:{couponId}:stock`
- 발급 사용자 Set 키: `coupon:{couponId}:users`
- 발급 상태 키: `coupon:{couponId}:user:{userId}:issue-status` (`PENDING`, `SUCCESS`, `FAILED`, TTL 10분)
- Lua Script: `src/main/resources/scripts/issue_coupon.lua`

Lua Script는 `SISMEMBER`로 기발급 여부를 확인하고, 미발급 사용자에 한해 `GET stock`으로 재고를 검증한다. 재고가 남아 있으면 `DECR`, `SADD`, `PENDING` 상태 기록을 같은 원자 연산에서 실행한다.

MySQL은 선착순 재고를 직접 조회하거나 차감하지 않는다.

## 3. 이벤트 및 영속화 흐름

1. 클라이언트가 `POST /api/coupons/{couponId}/issue`를 호출한다.
2. 애플리케이션이 Redis Lua Script로 사용자 중복과 재고를 원자적으로 검증한다.
3. 애플리케이션은 `CouponIssueEvent(couponId, userId, issuedAt)`를 `coupon.issue.request` 토픽으로 발행한다. 발행 실패 시 Redis `INCR`, `SREM`, 상태 `FAILED`로 즉시 보상한다.
4. Kafka Consumer가 이벤트를 수신해 MySQL `coupon_history`에 비동기 INSERT하고, 성공하면 Redis 상태를 `SUCCESS`로 변경한다.
5. 컨트롤러는 최대 5초 동안 최종 상태를 대기한다. `SUCCESS`면 발급 완료, `FAILED`면 발급 실패를 반환하며, 시간이 만료되면 `PENDING`을 반환한다.
6. `coupon_history`의 `(coupon_id, user_id)` Unique Constraint로 Redis Set과 함께 2중 멱등성을 보장한다.

## 4. 폴아웃 및 보상 흐름

1. DB 데드락, 유니크 제약 위반, 네트워크 오류 등 영속화 실패는 `@RetryableTopic(attempts = "2")`으로 최초 처리 후 1회 재시도한다.
2. 재시도 실패 이벤트는 `coupon.issue.dlt`로 격리한다.
3. `@DltHandler`의 Fallout Worker는 Redis 재고를 `INCR`하고, 발급 사용자 Set에서 `SREM`으로 이력을 롤백하며 상태를 `FAILED`로 변경한다.
4. 최종 실패 이벤트는 Kafka DLT 레코드, 구조화 감사 로그, Slack Webhook으로 남긴다.

이 보상은 Redis에서 이미 차감된 수량이 DB 쓰기 실패로 유실되지 않도록 보장한다.

## 5. 검증 경계

- `RedisConcurrencyTest`: 재고 100개, 동시 요청 1,000건에서 성공 100건과 재고 0을 검증한다.
- `CouponConsumerIntegrationTest`: Kafka 이벤트 발행 후 MySQL 적재를 검증한다.
- `KafkaFalloutIntegrationTest`: DB 실패 이후 DLT 라우팅과 Redis 재고·사용자 Set 복구를 검증한다.
- `CouponIssueProducerFailureTest`: Kafka 발행 실패 시 Redis 보상과 `FAILED` 상태 기록을 검증한다.
- 최종 상태가 5초 안에 결정되지 않으면 `PENDING`을 반환하고 상태 조회 API로 전환되는지 검증한다.
