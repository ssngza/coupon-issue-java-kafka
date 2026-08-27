# 선착순 쿠폰 발급 & 폴아웃 처리 시스템

Redis의 원자적 선착순 발급과 Kafka 기반 비동기 영속화, DLT 보상 처리를 구현하는 Spring Boot 프로젝트입니다.

## 핵심 구성

- Redis Lua Script: 재고 차감과 사용자 중복 발급 방지
- Kafka: 발급 이벤트 비동기 전달 및 MySQL 영속화
- DLT: 최종 영속화 실패 시 Redis 재고와 사용자 발급 이력 복구

## 문서

- [하네스 구현 명세](HARNESS_SPEC.md)
- [시스템 아키텍처](ARCHITECTURE.md)
- [설계 문서 색인](docs/design-docs/index.md)
- [제품 명세 색인](docs/product-specs/index.md)
- [구현 로드맵](docs/PLANS.md)

상세 기술 스택, 핵심 불변 규칙, 단계별 구현·테스트 기준, API 및 DDL은 [HARNESS_SPEC.md](HARNESS_SPEC.md)를 기준으로 합니다.
