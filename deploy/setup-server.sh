#!/bin/bash
# =============================================================
# GUIDEON 서버 초기 세팅 스크립트
# EC2 Ubuntu 22.04 LTS 기준
# 사용법: chmod +x setup-server.sh && ./setup-server.sh
# =============================================================

set -e

echo "========================================="
echo "  GUIDEON Server Setup"
echo "========================================="

# 1. 시스템 업데이트
echo "[1/6] System update..."
sudo apt update && sudo apt upgrade -y

# 2. Docker 설치
echo "[2/6] Installing Docker..."
sudo apt install -y ca-certificates curl gnupg
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# 3. Docker 권한 설정
echo "[3/6] Configuring Docker permissions..."
sudo usermod -aG docker $USER

# 4. 배포 디렉토리 설정
echo "[4/6] Setting up deploy directory..."
mkdir -p ~/guideon-deploy
cd ~/guideon-deploy

echo "[!] deploy 파일을 여기에 복사해야 합니다:"
echo "    ~/guideon-deploy/docker-compose.prod.yml"
echo "    ~/guideon-deploy/nginx.conf"
echo "    ~/guideon-deploy/.env"

# 5. Swap 설정 (t3.small 2GB 메모리 보조)
echo "[5/6] Setting up 2GB swap..."
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# 6. 방화벽 설정
echo "[6/6] Configuring firewall..."
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 80/tcp    # HTTP
sudo ufw allow 443/tcp   # HTTPS
sudo ufw --force enable

echo ""
echo "========================================="
echo "  Setup complete!"
echo "========================================="
echo ""
echo "다음 단계:"
echo "  1. 로그아웃 후 재로그인 (Docker 권한 적용)"
echo "  2. ~/guideon-deploy/ 에 파일 배치:"
echo "     - docker-compose.prod.yml → docker-compose.yml 로 이름 변경"
echo "     - nginx.conf"
echo "     - .env (실제 값 채우기)"
echo "  3. DuckDNS 도메인을 이 서버 IP로 설정"
echo "  4. GHCR 로그인:"
echo "     docker login ghcr.io -u <github-username>"
echo "  5. 인증서 발급 (HTTP 먼저 동작 확인 후):"
echo "     docker compose run --rm certbot certonly \\"
echo "       --webroot -w /var/www/certbot \\"
echo "       -d guideon.duckdns.org \\"
echo "       --email your@email.com --agree-tos --no-eff-email"
echo "  6. nginx.conf에서 HTTPS 블록 주석 해제"
echo "  7. 서비스 시작:"
echo "     docker compose up -d"
echo ""
