# System Reliability & Fault Tolerance

## 1. 장애 시나리오 및 대응

1. **Redis OOM 방지**: `maxmemory 256mb` 및 `allkeys-lru` 정책 설정.
2. **Kafka Consumer Deadlock**: Spring Kafka 백오프 재시도(3회) 후 DLT로 안전하게 우회.
3. **EC2 OOM Killer 대응**: 2GB Swap Memory 활성화로 JVM/Kafka 메모리 스파이크 완충.
