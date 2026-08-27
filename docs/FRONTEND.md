# Frontend Specification

## 1. 아키텍처

- 별도 Node.js 빌드 툴체인(Vite, Webpack 등) 없이 단일 `index.html` 파일로 작성
- Nginx 컨테이너의 `/usr/share/nginx/html/` 경로에 마운트하여 정적 파일 서빙

## 2. API Contract

- `GET /api/coupons/{id}/stock` -> `{ "stock": 42 }`
- `POST /api/coupons/{id}/issue` -> Body: `{ "userId": 1001 }` -> Response: `{ "success": true, "message": "발급 완료" }`
