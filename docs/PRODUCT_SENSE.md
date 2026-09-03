# Product Sense & Business Logic

## 1. 사용자 경험(UX) 보장

- 선착순 이벤트 특성상 사용자는 최종 발급 결과를 명확히 확인해야 한다.
- Redis 검증 성공은 `PENDING`일 뿐 발급 완료가 아니다. API는 최대 5초 동안 MySQL 영속화 또는 DLT 보상 결과를 기다린다.
- 5초 안에 결과가 없으면 `PENDING`을 반환하고 UI는 상태 조회 API로 최종 결과를 표시한다.

## 2. 비즈니스 신뢰성

- Kafka 발행 실패 또는 DB 오류는 Redis 재고를 `INCR`하고 발급 사용자 Set을 `SREM`하여 보상한다. 최종 실패는 `FAILED`로 사용자에게 표시하며, Kafka DLT 레코드와 구조화 감사 로그로 실패 원인·보상 결과를 남긴다.
