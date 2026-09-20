#!/bin/bash
set -euo pipefail

# EC2 최초 생성 시 한 번 실행합니다. 이후 배포는 SSH 대신 GitHub Actions와 SSM이 담당합니다.
dnf update -y
dnf install -y docker git
systemctl enable --now docker
systemctl enable --now amazon-ssm-agent || true
usermod -aG docker ec2-user

mkdir -p /usr/local/lib/docker/cli-plugins
ARCH=$(uname -m)
case "$ARCH" in
  x86_64) COMPOSE_ARCH=x86_64 ;;
  aarch64) COMPOSE_ARCH=aarch64 ;;
  *) echo "Unsupported architecture: $ARCH" >&2; exit 1 ;;
esac
curl -SL "https://github.com/docker/compose/releases/download/v2.30.3/docker-compose-linux-$COMPOSE_ARCH" \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
mkdir -p /opt/coupon-issue-java-kafka
chown -R ec2-user:ec2-user /opt/coupon-issue-java-kafka
