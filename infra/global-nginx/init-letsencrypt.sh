#!/usr/bin/env bash
# Let's Encrypt 초기 인증서 발급.
# 한 번만 실행하면 됨. 이후 갱신은 docker compose 의 certbot 컨테이너가 자동 처리.
#
# 사전 조건:
#   - burty.co.kr / www.burty.co.kr A 레코드가 이 EC2 IP 로 등록되어 있음
#   - 80/443 포트가 EC2 Security Group 에서 열려 있음
#   - 이전 nginx-proxy / acme-companion 컨테이너 중지·삭제 완료
#
# 동작:
#   1) 자체 서명 더미 인증서로 nginx 우선 부팅 (cert 파일 없으면 nginx 가 안 뜸 → chicken-and-egg)
#   2) certbot 으로 진짜 인증서 발급 (HTTP-01 challenge via webroot)
#   3) 더미 제거 + nginx reload
#
# Idempotent — 다시 실행해도 안전.

set -euo pipefail

DOMAINS=(burty.co.kr www.burty.co.kr)
EMAIL="dhxogns920@gmail.com"
STAGING=0   # 1 로 두면 Let's Encrypt staging 사용 (rate limit 회피, 브라우저 신뢰 X)

if [ "$EUID" -ne 0 ] && ! groups | grep -qE '(^|[[:space:]])(docker)([[:space:]]|$)'; then
    echo "❌ docker 그룹에 속해 있지 않음. sudo 로 실행하거나 docker 그룹에 추가."
    exit 1
fi

COMPOSE="docker compose -f $(dirname "$0")/docker-compose.yml"

echo "▶ 1. 더미 인증서 생성 (nginx 부팅용)"
DUMMY_DIR=/etc/letsencrypt/live/${DOMAINS[0]}
$COMPOSE run --rm --entrypoint "/bin/sh -c '\
  mkdir -p ${DUMMY_DIR} && \
  openssl req -x509 -nodes -newkey rsa:4096 -days 1 \
    -keyout ${DUMMY_DIR}/privkey.pem \
    -out    ${DUMMY_DIR}/fullchain.pem \
    -subj /CN=localhost'" certbot

echo "▶ 2. nginx 부팅"
$COMPOSE up -d global-nginx
sleep 3

echo "▶ 3. 더미 인증서 삭제"
$COMPOSE run --rm --entrypoint "rm -rf /etc/letsencrypt/live/${DOMAINS[0]} /etc/letsencrypt/archive/${DOMAINS[0]} /etc/letsencrypt/renewal/${DOMAINS[0]}.conf" certbot

echo "▶ 4. 실제 인증서 발급"
DOMAIN_ARGS=""
for d in "${DOMAINS[@]}"; do
    DOMAIN_ARGS="$DOMAIN_ARGS -d $d"
done

STAGING_ARG=""
if [ $STAGING -eq 1 ]; then
    STAGING_ARG="--staging"
fi

$COMPOSE run --rm --entrypoint "certbot certonly \
    --webroot -w /var/www/certbot \
    --email $EMAIL --agree-tos --no-eff-email \
    --force-renewal \
    $STAGING_ARG \
    $DOMAIN_ARGS" certbot

echo "▶ 5. nginx reload"
$COMPOSE exec global-nginx nginx -s reload

echo "▶ 6. certbot 자동 갱신 컨테이너 기동"
$COMPOSE up -d certbot

echo ""
echo "✅ 완료. 다음 확인:"
echo "   curl -sI https://${DOMAINS[0]}/health"
echo "   echo | openssl s_client -servername ${DOMAINS[0]} -connect ${DOMAINS[0]}:443 2>/dev/null | openssl x509 -noout -issuer -dates"
