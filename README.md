# 선착순 쿠폰 발급 & 폴아웃 처리 시스템

Redis의 원자적 선착순 발급과 Kafka 기반 비동기 영속화, DLT 보상 처리를 구현하는 Spring Boot 프로젝트입니다.

## 핵심 구성

- Redis Lua Script: 재고 차감과 사용자 중복 발급 방지
- Kafka: 발급 이벤트 비동기 전달 및 MySQL 영속화
- DLT: 최종 영속화 실패 시 Redis 재고와 사용자 발급 이력 복구

## 문서

- **하네스 구현 명세**: Redis 원자 처리, Kafka 비동기 영속화, DLT 보상 및 단계별 테스트 완료 기준을 정의합니다. [상세 보기](HARNESS_SPEC.md)
- **시스템 아키텍처**: Nginx, Spring Boot, Redis, Kafka, MySQL의 책임과 이벤트·보상 흐름을 설명합니다. [상세 보기](ARCHITECTURE.md)
- **현재 프로젝트 구조**: 저장소에 실제로 존재하는 문서와 배포 자산의 구조를 기록합니다. [상세 보기](PROJECT_STRUCTURE.md)
- **설계 문서**: 불변 규칙, 구현 세부 전략, 데이터베이스 DDL을 제공합니다. [설계 문서 색인](docs/design-docs/index.md)
- **제품 명세**: 테스트 클라이언트의 재고 조회, 발급 요청, 동시성 시뮬레이션 요구사항을 제공합니다. [제품 명세 색인](docs/product-specs/index.md)
- **구현 로드맵**: Redis, Kafka, DLT, 부하 테스트의 마일스톤을 추적합니다. [상세 보기](docs/PLANS.md)

상세 기술 스택, 핵심 불변 규칙, 단계별 구현·테스트 기준, API 및 DDL은 [HARNESS_SPEC.md](HARNESS_SPEC.md)를 기준으로 합니다.
