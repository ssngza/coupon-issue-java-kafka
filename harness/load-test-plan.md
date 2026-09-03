# 부하 테스트 계획

## 실행

1. `docker compose -f docker/docker-compose.yml up -d`로 Redis, Kafka, MySQL을 기동한다.
2. 애플리케이션을 빌드하고 `APP_IMAGE`로 지정해 Compose 앱을 실행한다.
3. JMeter 또는 `harness/scripts/test_concurrency.py`로 쿠폰 ID 하나에 1,000명의 발급 요청을 동시에 전송한다.
4. 실행 후 `./gradlew test`로 회귀 테스트를 수행한다.

## 합격 기준

- 재고 100개, 동시 요청 1,000건에서 최종 `SUCCESS`는 정확히 100건이다.
- `(coupon_id, user_id)` 중복 영속화가 0건이며 초과 발급도 0건이다.
- Redis 재고와 성공한 `coupon_history` 건수의 합이 초기 재고와 일치한다.
- DLT 발생 건은 Redis 사용자 Set에서 제거되고 재고가 1건씩 복구된다.
- 모든 요청은 `SUCCESS`, `FAILED`, `PENDING`, `DUPLICATE`, `OUT_OF_STOCK` 중 하나의 상태로 종료된다.

## 관찰 항목

- Kafka Consumer lag과 retry/DLT 토픽 유입량
- Redis stock 값과 users Set 크기
- MySQL `coupon_history` 유니크 제약 위반 수
- API p95/p99 응답 시간과 오류율
