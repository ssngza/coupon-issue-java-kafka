# 단일 EC2 간이 배포 Runbook

단일 EC2에서 Nginx, Spring Boot, Redis, Kafka, MySQL을 Docker Compose로 실행합니다. 외부 공개 포트는 Nginx만 사용하며 MySQL 데이터는 `mysql-data` named volume에 저장합니다.

## 1. 사전 조건

- EC2에 Docker Engine과 Docker Compose Plugin 설치
- 보안 그룹 인바운드: `22`(관리자 IP만), `80`(서비스 사용자)
- `3306`, `6379`, `9092`, `8080`은 외부에 개방하지 않음
- 충분한 디스크 공간과 Swap 설정 확인

## 2. 최초 배포

```bash
git clone https://github.com/ssngza/coupon-issue-java-kafka.git
cd coupon-issue-java-kafka
cp docker/.env.deploy.example docker/.env
vi docker/.env
docker compose --env-file docker/.env -f docker/docker-compose.yml config --quiet
docker compose --env-file docker/.env -f docker/docker-compose.yml up -d --build
docker compose --env-file docker/.env -f docker/docker-compose.yml ps
```

`docker/.env`의 `replace-with-...` 값을 긴 임의의 값으로 교체합니다. 이 파일은 커밋하지 않습니다.

```bash
curl -fsS http://localhost/api/coupons/1/stock
```

## 3. 업데이트

```bash
git pull --ff-only
docker compose --env-file docker/.env -f docker/docker-compose.yml config --quiet
docker compose --env-file docker/.env -f docker/docker-compose.yml up -d --build
docker compose --env-file docker/.env -f docker/docker-compose.yml ps
```

`down -v`, `volume prune`, `system prune --volumes`는 실행하지 않습니다. MySQL과 Redis 데이터를 삭제할 수 있습니다.

## 4. MySQL 백업 및 복구

백업 파일은 EC2 외부 저장소에도 복사해야 합니다.

```bash
mkdir -p backups
docker compose --env-file docker/.env -f docker/docker-compose.yml exec -T mysql sh -c 'mysqldump -ucoupon -p"$MYSQL_PASSWORD" --single-transaction coupon_db' > "backups/coupon_db-$(date +%Y%m%d-%H%M%S).sql"
```

복구 전에는 애플리케이션을 중지하고 백업 파일을 별도로 보존합니다.

```bash
docker compose --env-file docker/.env -f docker/docker-compose.yml stop app nginx
docker compose --env-file docker/.env -f docker/docker-compose.yml exec -T mysql sh -c 'mysql -ucoupon -p"$MYSQL_PASSWORD" coupon_db' < backups/backup.sql
docker compose --env-file docker/.env -f docker/docker-compose.yml start app nginx
```

## 5. 장애 확인

```bash
docker compose --env-file docker/.env -f docker/docker-compose.yml ps
docker compose --env-file docker/.env -f docker/docker-compose.yml logs --tail=200 app
docker compose --env-file docker/.env -f docker/docker-compose.yml logs --tail=100 mysql redis kafka
```

DLT 장애는 [DLT 운영 Runbook](dlt-runbook.md)의 정합성 확인과 단건 재처리 절차를 따릅니다.

## 6. 간이 배포의 한계

- 단일 EC2 장애 시 서비스 전체가 중단됩니다.
- Kafka와 Redis를 단일 인스턴스에서 운영하므로 고가용성이 없습니다.
- named volume은 서버 장애를 대체하지 않으므로 MySQL 백업을 별도 저장소에 복사해야 합니다.
- 트래픽 증가나 무중단 배포가 필요해지면 RDS, 관리형 Redis/Kafka 또는 ECS 전환을 검토합니다.
