# System Architecture

선착순 쿠폰 발급 및 폴아웃 처리 시스템의 전체 아키텍처 명세서입니다.

## 1. High-Level Architecture

- **API Entrypoint**: Nginx (Reverse Proxy & Static Web Client Hosting)
- **Application Server**: Spring Boot 3.x (Producer, Consumer, Fallout Worker 통합 단일 인스턴스)
- **Fast-path Tier**: Redis (Lua Script 기반 원자적 수량 검증 및 중복 발급 차단)
- **Event Streaming Tier**: Apache Kafka (KRaft Mode, 단일 브로커, DLT 격리)
- **Persistence Tier**: AWS RDS for MySQL (최종 영속 저장소)

## 2. Data Flow

1. **Client Request**: `POST /api/coupons/{couponId}/issue`
2. **Atomic Verification**: Redis Lua Script 실행 (`SISMEMBER` 검증 -> `GET stock` -> `DECR` 및 `SADD`)
3. **Event Publish**: `CouponIssueEvent`를 Kafka `coupon.issue.request` 토픽으로 발행
4. **Async Persist**: Kafka Consumer가 이벤트를 수신하여 MySQL `coupon_history` 테이블에 INSERT
5. **Fallout Compensation**: DB 쓰기 실패(Deadlock, Unique Key Conflict 등) 발생 시 `@RetryableTopic` 거쳐 DLT로 이동 -> Fallout Worker가 Redis 재고 복구(`INCR`) 및 사용자 발급 이력 롤백(`SREM`)
