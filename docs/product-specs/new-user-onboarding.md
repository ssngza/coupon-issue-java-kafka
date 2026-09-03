# Product Spec: 선착순 쿠폰 발급 클라이언트 인터페이스

## 1. 개요

브라우저에서 테스트할 수 있는 경량 정적 SPA 웹 UI 인터페이스 명세입니다.

## 2. 주요 기능 요구사항

- **실시간 재고 조회**: 2초 간격 Polling을 통해 `GET /api/coupons/{couponId}/stock`의 Redis 잔여 수량을 표시
- **단일 발급 요청**: 사용자 ID를 입력하여 `POST /api/coupons/{couponId}/issue`를 호출하고 최대 5초 동안 최종 발급 결과를 대기
- **다중 동시 요청 시뮬레이션**: 10명의 가상 유저 ID를 생성하여 0.1초 내 동시 발급 요청 트리거
- **처리 중 상태 확인**: `PENDING` 응답이면 상태 조회 API를 폴링해 최종 결과를 확인
- **실시간 로그 창**: 최종 `SUCCESS`, `FAILED` 또는 `PENDING` 상태와 메시지를 콘솔 뷰 형태로 출력

`SUCCESS`는 MySQL 영속화까지 끝난 발급 완료다. Kafka 발행 실패 또는 DLT 최종 실패는 Redis 보상 후 `FAILED`로 표시하며, `PENDING`은 실패가 아니라 아직 최종 결과가 결정되지 않은 상태다.
