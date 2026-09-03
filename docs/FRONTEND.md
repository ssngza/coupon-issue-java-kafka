# Frontend Specification

## 1. 아키텍처

- 별도 Node.js 빌드 툴체인(Vite, Webpack 등) 없이 단일 `index.html` 파일로 작성
- Nginx 컨테이너의 `/usr/share/nginx/html/` 경로에 마운트하여 정적 파일 서빙

## 2. API Contract

- `GET /api/coupons/{couponId}/stock` -> `{ "stock": 42 }`
- `POST /api/coupons/{couponId}/issue` -> Body: `{ "userId": 1001 }` -> 최대 5초 대기 후 `SUCCESS`, `FAILED`, 또는 `PENDING` 응답
- `GET /api/coupons/{couponId}/issues/{userId}/status` -> `{ "status": "PENDING" | "SUCCESS" | "FAILED", "message": "..." }`

`SUCCESS`는 MySQL 영속화까지 완료된 최종 발급 성공을 의미한다. `FAILED`는 Kafka 발행 또는 DLT 최종 실패 뒤 Redis 재고·발급 이력이 보상된 상태를 의미한다. `PENDING`이면 UI는 상태 조회 API를 폴링해 최종 결과만 성공 또는 실패로 표시한다.
