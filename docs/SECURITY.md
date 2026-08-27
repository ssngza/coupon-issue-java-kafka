# Security & Access Control

## 1. 네트워크 보안

- Nginx(Port 80/443)만 외부에 노출하고 Spring Boot(8080), Redis(6379), Kafka(9092)는 Docker 내부 브리지 네트워크로 격리.
- AWS RDS MySQL은 Security Group을 통해 EC2 인스턴스의 IP에서만 3306 포트 인바운드 허용.

## 2. 데이터 유효성 검증

- 모든 API 요청은 Spring Boot Validation(`@Valid`, `@NotNull`, `@Positive`)을 통해 이상 파라미터를 1차 차단.
- 쿠폰 발급 요청의 `userId`와 경로의 `couponId`는 유효성 검증을 통과한 뒤에만 Redis Lua Script에 전달한다.
