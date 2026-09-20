# SSH 없는 EC2 배포

이 프로젝트는 GitHub Actions의 수동 workflow가 AWS Systems Manager(SSM) Run Command로 EC2에 배포하는 방식을 지원합니다. SSH 키와 SSH 포트는 사용하지 않습니다.

## 1. EC2 준비

Amazon Linux 2023 EC2를 생성할 때 [EC2 User Data](../../deploy/ec2-user-data-amazon-linux.sh)를 넣습니다. 인스턴스에는 다음 IAM 권한이 필요합니다.

- `AmazonSSMManagedInstanceCore`
- `/coupon/prod/*` 경로의 SSM Parameter Store SecureString 읽기 권한

인스턴스가 SSM 콘솔에서 `Online` 상태가 된 뒤 배포할 수 있습니다.

## 2. 보안 그룹

- 인바운드 `8081/tcp`: 테스트를 허용할 사용자 IP 또는 필요한 범위
- `22/tcp`: 열지 않음
- `3306`, `6379`, `9092`, `8080`: 열지 않음

배포 후 접근 주소는 다음 형식입니다.

```text
http://EC2_PUBLIC_IP:8081/
```

## 3. SSM Parameter Store

다음 이름으로 SecureString 파라미터를 생성합니다.

```text
/coupon/prod/APP_IMAGE
/coupon/prod/NGINX_HTTP_PORT
/coupon/prod/MYSQL_DATABASE
/coupon/prod/MYSQL_USER
/coupon/prod/MYSQL_PASSWORD
/coupon/prod/MYSQL_ROOT_PASSWORD
/coupon/prod/COUPON_ADMIN_TOKEN
```

`NGINX_HTTP_PORT`는 `8081`로 설정합니다. 비밀번호와 관리자 토큰은 저장소에 커밋하지 않습니다.

## 4. GitHub Actions 실행

저장소의 `Actions > Deploy to EC2 via SSM > Run workflow`에서 다음 값을 입력합니다.

- `instance_id`: EC2 인스턴스 ID
- `aws_region`: EC2 리전, 예: `ap-northeast-2`

GitHub Repository Secrets에는 다음 두 값만 등록합니다.

- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`

이 workflow는 SSM으로 EC2에 명령을 보내 `main` 코드를 가져오고, SSM Parameter Store에서 `docker/.env`를 생성한 뒤 Compose를 재기동합니다.

## 5. 배포 확인

```bash
curl -fsS http://EC2_PUBLIC_IP:8081/api/coupons/1/stock
```

관리자 재고 설정은 UI의 관리자 토큰 입력란에 `/coupon/prod/COUPON_ADMIN_TOKEN` 값을 입력합니다.

실패 시 GitHub Actions 로그의 SSM 표준 출력과 EC2 내부 Compose 로그를 확인합니다. 데이터 백업과 장애 복구는 [단일 EC2 배포 Runbook](single-ec2-deploy.md)을 따릅니다.
