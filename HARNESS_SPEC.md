# [Harness Engineering Spec] 선착순 쿠폰 발급 & 폴아웃 처리 시스템

본 문서는 Codex 등 AI 코딩 에이전트가 요구사항 명세, 제약 조건, 테스트 기준을 기반으로 코드를 안전하고 점진적으로 생성·검증(Harness Driven)할 수 있도록 정의한 실행 명세서입니다.

---

## 1. 시스템 아키텍처 및 핵심 규칙

### 1.1 기술 스택

- **Language/Framework**: Java 17, Spring Boot 3.x
- **Data Tier**: AWS RDS MySQL (Master Entity), Redis (Atomic Counter & Set), Apache Kafka (KRaft Mode)
- **Quality & Harness**: JUnit5, AssertJ, Mockito, Testcontainers, JMeter

### 1.2 핵심 불변 규칙 (Invariant Rules for AI)

1. **DB 직접 차감 금지**: 모든 선착순 수량 검증 및 차감은 Redis 내에서 원자적으로 처리되어야 함.
2. **동시성 멱등성**: 유저 1명당 쿠폰 1회 발급 보장 (`SETNX` 또는 Redis Set 활용).
3. **폴아웃(Fallout) 보상 보장**: Kafka Consumer에서 DB 쓰기 실패 시 DLT로 격리하고, Redis 차감 수량을 반드시 복구(`INCR`)해야 함.
4. **최종 결과 응답**: Redis 성공만으로 발급 완료를 응답하지 않는다. Kafka 영속화 성공 또는 DLT 최종 실패까지 처리한 결과를 사용자에게 반환한다.

### 1.3 Kafka 토픽 규약

- `coupon.issue.request`: Redis 발급 성공 이벤트 스트림
- `coupon.issue.retry`: 최초 영속화 실패 후 1회 재시도 이벤트 스트림
- `coupon.issue.dlt`: 재시도 후 최종 영속화 실패 메시지 격리

---

## 2. 점진적 구현 단계 (Codex Prompting Phase)

### [Phase 1] Redis 원자적 선착순 발급 로직 구축

* **목표**: Redis Lua Script를 활용해 중복 발급 방지, 재고 차감, `PENDING` 상태 기록을 1회의 원자적 연산으로 처리.
* **Codex 지시 사항**:
  - `CouponRedisRepository` 생성.
  - Lua Script 파일 정의 (`scripts/issue_coupon.lua`).
  - 요청 유저의 기발급 여부 확인 (`SISMEMBER`) 후 미발급 시 수량(`GET stock`) 확인 및 차감(`DECR`) + 유저 추가(`SADD`) + `PENDING` 상태 기록.
* **검증 테스트 (Harness Test)**:
  - `RedisConcurrencyTest.java`: 100개 한정 쿠폰에 대해 1,000개의 쓰레드로 동시 요청 시 정확히 100명만 성공하고 재고가 0이 되는지 검증.

---

### [Phase 2] Kafka 이벤트 프로듀서 & 컨슈머 파이프라인

* **목표**: Redis 발급 성공 건을 Kafka 토픽(`coupon.issue.request`)으로 전송하고, 컨슈머가 이를 읽어 RDS MySQL에 비동기 INSERT한 뒤 최종 결과를 Redis 상태 키에 기록.
* **Codex 지시 사항**:
  - `CouponIssueEvent` DTO 정의 (`couponId`, `userId`, `issuedAt`).
  - `KafkaTemplate`을 사용한 비동기 Producer 구현.
  - `@KafkaListener` 기반 Consumer 구현 및 `CouponHistory` 엔티티 저장 로직 작성.
  - `coupon.issue.request`, `coupon.issue.retry`, `coupon.issue.dlt` 토픽을 명시 이름으로 생성·사용.
  - Kafka 발행 실패 시 Redis 재고 `INCR`, 사용자 Set `SREM`, 상태 `FAILED` 기록 후 발급 실패로 처리.
  - MySQL 영속화 성공 시 상태를 `SUCCESS`로 기록.
* **검증 테스트 (Harness Test)**:
  - `CouponConsumerIntegrationTest.java`: Embedded Kafka 또는 Testcontainers를 띄워 이벤트 발행 후 DB에 정상 적재되는지 검증.

---

### [Phase 3] 폴아웃(Fallout) 및 DLT 보상 트랜잭션 구현

* **목표**: DB 데드락, 유니크 제약 위반, 네트워크 장애 시 최초 처리 1회와 재시도 1회 후 DLT 격리 및 Redis 재고 복구.
* **Codex 지시 사항**:
  - Spring Kafka `@RetryableTopic(attempts = "2", backoff = @Backoff(delay = 1000))` 적용.
  - `@DltHandler` 구현: 최종 실패 메시지를 소비하여 Redis 수량 복구(`INCR`) 및 유저 발급 이력 롤백(`SREM`).
  - DLT 처리 시 상태를 `FAILED`로 기록하고, Kafka DLT 레코드·구조화 감사 로그·Slack Webhook 알림을 남김.
* **검증 테스트 (Harness Test)**:
  - `KafkaFalloutIntegrationTest.java`: 의도적으로 DB Exception을 유발한 후 DLT로 라우팅되고 Redis 수량이 원래대로 복구되는지 검증.

---

## 3. API 및 데이터베이스 명세

### 3.1 REST API 명세

| Endpoint | Method | Body / Param | Description |
| :--- | :--- | :--- | :--- |
| `/api/coupons/{couponId}/stock` | GET | - | 현재 잔여 수량 조회 (Redis) |
| `/api/coupons/{couponId}/issue` | POST | `{"userId": 1001}` | 최대 5초 동안 최종 발급 결과 대기. `SUCCESS`, `FAILED`, 또는 `PENDING` 반환 |
| `/api/coupons/{couponId}/issues/{userId}/status` | GET | - | Redis 발급 상태(`PENDING`, `SUCCESS`, `FAILED`) 및 사용자 메시지 조회 |

### 3.2 DDL 스키마 (`schema.sql`)

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
    status VARCHAR(20) NOT NULL, -- 'SUCCESS', 'FAILED'
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_coupon_user UNIQUE (coupon_id, user_id)
);
```

---

## 4. 부하 테스트 및 검증 시나리오

```bash
# 1. 인프라 기동 (Local Docker Compose)
docker-compose -f docker/docker-compose.yml up -d

# 2. JUnit 단위/통합 테스트 하네스 실행
./gradlew test

# 3. Python/JMeter 동시성 부하 테스트 실행 (1,000 유저 동시 요청)
python harness/scripts/test_concurrency.py --users 1000 --coupon-id 1

# 4. 정합성 검증 쿼리 실행
# Redis 잔여 수량 + MySQL INSERT 성공 건수 == Coupon 총 발행 수량 확인
```

## 5. 응답 대기 및 실패 감사 정책

- 상태 키: `coupon:{couponId}:user:{userId}:issue-status`
- 상태 값: `PENDING`, `SUCCESS`, `FAILED`
- 상태 TTL: 10분
- `POST /issue`는 최대 5초 동안 최종 상태를 대기한다.
- 5초 안에 최종 상태가 없으면 `PENDING`을 반환하고 클라이언트는 상태 조회 API를 폴링한다.
- 실패 원본은 Kafka DLT 레코드로 보존하고, 애플리케이션은 쿠폰·사용자·실패 단계·예외·보상 결과를 구조화 로그로 기록한다.
