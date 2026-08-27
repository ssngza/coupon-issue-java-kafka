# Technical Debt Tracker

| ID | 구분 | 내용 | 영향도 | 해결 계획 |
| :--- | :--- | :--- | :--- | :--- |
| TD-001 | Infrastructure | 단일 EC2 인스턴스 사용으로 인한 Single Point of Failure (SPOF) | Medium | 향후 트래픽 증가 시 ECS Fargate 및 Managed Service로 단계적 이전 |
| TD-002 | Monitoring | 모니터링 APM 부재로 CLI 로그 기반 디버깅 필요 | Low | Prometheus + Grafana 경량 모니터링 컨테이너 추가 검토 |
