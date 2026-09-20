# DLT 운영 감시 및 수동 재처리 Runbook

이 문서는 대시보드 없이 `coupon.issue.dlt`에 남은 최종 실패 이벤트를 확인하고 복구하는 최소 절차입니다.

## 1. 장애 확인

### 애플리케이션 로그 확인

DLT 처리 결과는 다음 구조화 로그로 기록됩니다.

```text
coupon_issue_dlt couponId=1 userId=4717 exceptionType=... exceptionMessage=... compensation=SUCCESS
```

- `compensation=SUCCESS`: Redis 재고와 사용자 발급 Set 보상 완료
- `compensation=FAILED`: 보상 실패 상태이므로 재처리 전 Redis와 DB 상태를 확인

```powershell
docker compose -f docker/docker-compose.yml logs app | Select-String "coupon_issue_dlt"
```

### DLT 원본 확인

```powershell
docker compose -f docker/docker-compose.yml exec kafka `
  /opt/kafka/bin/kafka-console-consumer.sh `
  --bootstrap-server kafka:9092 `
  --topic coupon.issue.dlt `
  --from-beginning `
  --max-messages 20
```

운영 환경에서는 출력 결과를 파일로 보관하고, 같은 `couponId`와 `userId`가 중복 처리되지 않았는지 먼저 확인합니다.

## 2. 재처리 전 정합성 확인

### MySQL 발급 이력 확인

```sql
SELECT id, coupon_id, user_id, status, created_at
FROM coupon_history
WHERE coupon_id = 1 AND user_id = 4717;
```

### Redis 상태 확인

```powershell
docker compose -f docker/docker-compose.yml exec redis redis-cli GET coupon:1:stock
docker compose -f docker/docker-compose.yml exec redis redis-cli SISMEMBER coupon:1:users 4717
docker compose -f docker/docker-compose.yml exec redis redis-cli GET coupon:1:user:4717:issue-status
```

판단 기준은 다음과 같습니다.

- DB에 `SUCCESS`가 있으면 같은 이벤트를 재처리하지 않습니다.
- DB 이력이 없고 Redis 사용자 Set에 남아 있으면 보상 누락 가능성이 있으므로 먼저 `SREM`과 `INCR`의 필요성을 검토합니다.
- Redis 상태가 `FAILED`이면 이미 보상된 이벤트이므로 재처리하지 않습니다.

## 3. 수동 재처리

자동 재시도가 모두 끝난 이벤트만 운영자가 원인을 해결한 뒤 재처리합니다. 원본 DLT 메시지를 그대로 일괄 재전송하지 말고, 정합성 확인을 마친 이벤트만 아래 형식으로 한 건씩 전송합니다.

```powershell
docker compose -f docker/docker-compose.yml exec -T kafka `
  /opt/kafka/bin/kafka-console-producer.sh `
  --bootstrap-server kafka:9092 `
  --topic coupon.issue.request `
  --property parse.key=true `
  --property key.separator="|"
```

입력 예시:

```text
1|{"couponId":1,"userId":4717,"issuedAt":"2026-09-20T00:00:00Z"}
```

재처리 후 다음을 확인합니다.

1. 애플리케이션 로그에 DB 저장 및 `SUCCESS` 처리가 기록되는지 확인합니다.
2. `coupon_history`에 `(coupon_id, user_id)`가 한 건만 존재하는지 확인합니다.
3. Redis 상태가 `SUCCESS`인지 확인합니다.
4. 동일 이벤트를 다시 보내지 않습니다. DB Unique Constraint와 Redis 멱등성은 안전장치이지 반복 재처리 방법이 아닙니다.

## 4. 복구 실패 시 조치

보상 실패가 반복되면 이벤트를 계속 재전송하지 않고 다음 정보를 보관한 뒤 개발자에게 에스컬레이션합니다.

- DLT 원본 메시지
- `couponId`, `userId`
- 최초 DB 오류와 보상 오류 로그
- 재처리 시각과 수행 명령
- 재처리 전후 Redis 값과 MySQL 조회 결과
