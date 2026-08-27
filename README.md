# 선착순 쿠폰 발급 & 폴아웃 처리 시스템

이 저장소는 선착순 쿠폰 발급, Kafka 비동기 적재, 실패 보상 처리까지 포함한 하이브리드 발급 시스템을 구현하기 위한 프로젝트 문서와 구현 기준을 담습니다.

## 목표

- Redis 기반 원자적 선착순 발급
- Kafka 기반 비동기 이벤트 전송 및 MySQL 적재
- DLT 기반 폴아웃 처리 및 Redis 재고 복구
- 동시성, 멱등성, 실패 보상을 검증하는 테스트 하니스 구축

## 기술 스택

- Java 17
- Spring Boot 3.x
- AWS RDS MySQL
- Redis
- Apache Kafka KRaft
- JUnit 5
- AssertJ
- Mockito
- Testcontainers
- JMeter

## 핵심 불변 규칙

1. DB에서 재고를 직접 차감하지 않는다.
2. 쿠폰 발급은 사용자 1명당 1회만 허용한다.
3. Kafka Consumer의 DB 쓰기 실패 시 DLT로 격리하고 Redis 재고를 반드시 복구한다.

## 구현 단계

### Phase 1. Redis 원자적 선착순 발급

- `CouponRedisRepository` 구현
- Lua Script(`scripts/issue_coupon.lua`) 작성
- 중복 발급 방지와 재고 차감을 하나의 원자 연산으로 처리

검증:

- `RedisConcurrencyTest.java`
- 100개 재고에 대해 1,000개 동시 요청 시 정확히 100명만 성공해야 함
- 최종 재고는 0이어야 함

### Phase 2. Kafka 이벤트 파이프라인

- `CouponIssueEvent` DTO 정의
- `KafkaTemplate` 기반 Producer 구현
- `@KafkaListener` 기반 Consumer 구현
- `CouponHistory` 엔티티 저장 로직 작성

검증:

- `CouponConsumerIntegrationTest.java`
- 이벤트 발행 후 MySQL 적재 확인

### Phase 3. Fallout 및 DLT 보상 트랜잭션

- `@RetryableTopic(attempts = "3", backoff = @Backoff(delay = 1000, multiplier = 2.0))` 적용
- `@DltHandler` 구현
- Redis `INCR`, `SREM`으로 재고 및 발급 이력 복구
- 실패 감사 로그 및 Slack Webhook 알림 트리거

검증:

- `KafkaFalloutIntegrationTest.java`
- 의도적 DB Exception 발생 시 DLT 라우팅 및 Redis 복구 확인

## API 명세

| Endpoint | Method | Body / Param | Description |
| --- | --- | --- | --- |
| `/api/coupons/{couponId}/stock` | GET | - | 현재 잔여 수량 조회(Redis) |
| `/api/coupons/{couponId}/issue` | POST | `{"userId": 1001}` | 선착순 쿠폰 발급 요청 |

## 데이터베이스 스키마

```sql
CREATE TABLE IF NOT EXISTS coupon (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    total_quantity INT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS coupon_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coupon_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_coupon_user UNIQUE (coupon_id, user_id)
);
```

## 권장 디렉터리 예시

```text
src/
  main/
    java/
    resources/
      scripts/
        issue_coupon.lua
      schema.sql
  test/
    java/
```

## 개발 원칙

- Redis 검증과 차감은 항상 원자적으로 처리한다.
- Kafka 재시도와 DLT 처리는 보상 로직과 함께 설계한다.
- 동시성 테스트와 실패 복구 테스트를 먼저 작성한다.

