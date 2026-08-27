# Frontend Specification

## 1. 아키텍처

- 별도 Node.js 빌드 툴체인(Vite, Webpack 등) 없이 단일 `index.html` 파일로 작성
- Nginx 컨테이너의 `/usr/share/nginx/html/` 경로에 마운트하여 정적 파일 서빙

## 2. API Contract

- `GET /api/coupons/{couponId}/stock` -> `{ "stock": 42 }`
- `POST /api/coupons/{couponId}/issue` -> Body: `{ "userId": 1001 }` -> Response: `{ "success": true, "message": "발급 완료" }`

발급 성공 응답은 Redis 원자 검증 성공을 의미한다. Kafka Consumer의 MySQL 영속화와 DLT 보상은 응답 이후 비동기로 처리한다.
