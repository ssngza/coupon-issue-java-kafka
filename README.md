# 선착순 쿠폰 발급 & 폴아웃 처리 시스템

Redis의 원자적 선착순 발급과 Kafka 기반 비동기 영속화, DLT 보상 처리를 구현하는 Spring Boot 프로젝트입니다.

## 핵심 구성

- Redis Lua Script: 재고 차감과 사용자 중복 발급 방지
- Kafka: 발급 이벤트 비동기 전달 및 MySQL 영속화
- 최종 결과: 최대 5초 대기 후 `SUCCESS` 또는 `FAILED`를 반환하고, 지연 시 `PENDING` 상태 조회로 전환
- DLT: 최종 영속화 실패 시 Redis 재고와 사용자 발급 이력을 복구하고 감사 로그를 보존

## 문서

- **하네스 구현 명세**: Redis 원자 처리, Kafka 비동기 영속화, DLT 보상 및 단계별 테스트 완료 기준을 정의합니다. [상세 보기](HARNESS_SPEC.md)
- **시스템 아키텍처**: Nginx, Spring Boot, Redis, Kafka, MySQL의 책임과 이벤트·보상 흐름을 설명합니다. [상세 보기](ARCHITECTURE.md)
- **현재 프로젝트 구조**: 저장소에 실제로 존재하는 문서와 배포 자산의 구조를 기록합니다. [상세 보기](PROJECT_STRUCTURE.md)
- **설계 문서**: 불변 규칙, 구현 세부 전략, 데이터베이스 DDL을 제공합니다. [설계 문서 색인](docs/design-docs/index.md)
- **제품 명세**: 테스트 클라이언트의 재고 조회, 발급 요청, 동시성 시뮬레이션 요구사항을 제공합니다. [제품 명세 색인](docs/product-specs/index.md)
- **구현 로드맵**: Redis, Kafka, DLT, 부하 테스트의 마일스톤을 추적합니다. [상세 보기](docs/PLANS.md)

## 로컬 기동

`docker/.env.example`를 `docker/.env`로 복사한 뒤 아래 명령으로 인프라와 프록시를 실행합니다.

```bash
docker compose -f docker/docker-compose.yml up -d
```

기본 스택은 Nginx, App, Redis, Kafka, MySQL로 구성됩니다. `APP_IMAGE`는 애플리케이션 빌드가 완료된 뒤 사용합니다.

상세 기술 스택, 핵심 불변 규칙, 단계별 구현·테스트 기준, API 및 DDL은 [HARNESS_SPEC.md](HARNESS_SPEC.md)를 기준으로 합니다.
