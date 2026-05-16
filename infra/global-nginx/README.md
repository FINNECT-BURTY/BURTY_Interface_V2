# global-nginx

단일 nginx + certbot 로 구성한 글로벌 인그레스. `burty.co.kr` / `www.burty.co.kr`
의 모든 트래픽을 받아 path 기반으로 FE / BE 로 라우팅한다.

## 구조

```
                Internet
                   │
              burty.co.kr / www.burty.co.kr  (DNS A → 44.194.3.230)
                   │
        ┌──────────▼──────────┐
        │    global-nginx     │   80/443 노출, TLS 종단
        │   (nginx:1.27)      │
        └──────┬───────┬──────┘
               │       │     docker network: global-nginx
               │       │
    ┌──────────▼──┐ ┌──▼──────────┐
    │  frontend   │ │    burty    │
    │ (Next.js 등)│ │   :8080     │
    └─────────────┘ └─────────────┘

  /api/v1/*, /swagger-ui/*, /v3/api-docs, /webjars/*, /health → burty:8080
  /                                                       → frontend:80
```

`certbot` 컨테이너가 별도로 떠 있으며 12시간마다 인증서 갱신을 시도한다.

## 최초 셋업 (EC2 에서)

```bash
# 0. 기존 nginx-proxy + acme-companion 이 떠 있으면 중지·삭제
docker rm -f nginx-proxy acme-companion 2>/dev/null || true

# 1. 이 디렉토리 EC2 로 복사
cd <burty 프로젝트 루트>/infra/global-nginx

# 2. 인증서 부트스트랩 (한 번만)
./init-letsencrypt.sh

# 3. 확인
curl -sI https://burty.co.kr/health
```

## 일상 운영

```bash
# 기동 / 재기동
docker compose -f infra/global-nginx/docker-compose.yml up -d

# nginx 설정만 reload (cert 갱신 시 자동, 수동도 가능)
docker exec global-nginx nginx -s reload

# 로그
docker logs -f global-nginx
docker logs -f certbot
```

## BURTY / FE 컴포즈에서 이 네트워크에 가입하기

각 서비스의 `docker-compose.yml` 에 다음과 같이 외부 네트워크로 선언:

```yaml
services:
  burty:
    # ...
    networks:
      - global-nginx     # 서비스 이름이 곧 DNS hostname (burty 로 해석됨)

networks:
  global-nginx:
    external: true       # global-nginx 스택이 미리 만들어둔 네트워크 재사용
    name: global-nginx
```

서비스 이름이 `burty`, `frontend` 여야 nginx.conf 의 upstream 이 맞음. 다른 이름
쓰려면 `nginx.conf` 의 `upstream` 블록 같이 수정.

## 인증서 갱신 동작

- `certbot` 컨테이너가 12시간마다 `certbot renew --webroot` 실행
- Let's Encrypt 는 만료 30일 전부터 갱신 허용 (그전엔 no-op)
- 갱신 성공 시 cert 파일이 교체되고, `global-nginx` 도 12시간마다 `nginx -s reload`
  실행해서 새 cert 로딩

## 인증서 수동 발급/갱신/디버깅

```bash
# 강제 발급 (rate limit 주의 — 도메인 set 당 주 5회)
docker compose -f infra/global-nginx/docker-compose.yml run --rm \
  --entrypoint "certbot certonly --webroot -w /var/www/certbot --email dhxogns920@gmail.com \
                --agree-tos --no-eff-email --force-renewal -d burty.co.kr -d www.burty.co.kr" certbot

# 발급된 cert 목록
docker compose -f infra/global-nginx/docker-compose.yml run --rm \
  --entrypoint "certbot certificates" certbot

# 발급자 / 만료일 (브라우저 외부에서 빠르게 확인)
echo | openssl s_client -servername burty.co.kr -connect burty.co.kr:443 2>/dev/null \
  | openssl x509 -noout -issuer -dates -subject
```

## 도메인 추가 / 제거

`nginx.conf` 의 `server_name` 라인 + `init-letsencrypt.sh` 의 `DOMAINS` 배열 둘 다 갱신
후 nginx reload + cert 재발급.
