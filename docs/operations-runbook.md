# BURTY 운영 Runbook



## 로컬 전체 스택 기동



```bash

# global-nginx 네트워크 (최초 1회)

docker network create global-nginx 2>/dev/null || true



cd BURTY_Interface_V2

cp .env.example .env   # 값 채우기



docker compose -f docker-compose.yml -f docker-compose.local.yml up -d --build

```



| 서비스 | URL |

|--------|-----|

| API | http://localhost:8080/health |

| Prometheus | http://localhost:9090 |

| Grafana | http://localhost:3001 (admin/admin) |

| Loki | http://localhost:3100 (내부 — Grafana에서 조회) |



## 로그 보기 (Grafana + Loki)



1. 스택 기동 후 Grafana 접속: http://localhost:3001

2. **Dashboards → BURTY → BURTY 로그 대시보드** (`uid=burty-logs`)

3. 상단 `requestId` 입력으로 특정 요청 추적 (logback MDC와 동일 ID)

4. `level` 필터로 ERROR/WARN/INFO 구분



Promtail이 `HOST_LOG_DIR`(기본 `./logs`)의 `application.log`, `error.log` 를 Loki로 전송합니다.



### ADMIN API로 로그 대시보드 임베딩



```http

GET /api/v1/admin/observability/embed/dashboard?uid=burty-logs&theme=light&kiosk=true

GET /api/v1/admin/observability/embed/panel?uid=burty-logs&panelId=5

```



## DB 백업



```bash

DB_HOST=localhost DB_PASSWORD=... ./infra/scripts/backup-mariadb.sh

```



- 기본 보관: 14일 (`BACKUP_DIR`, `RETENTION_DAYS` 환경변수로 변경)

- 복구: `gunzip -c backups/burty_*.sql.gz | mysql -u root -p burty`



## CI/CD



- **PR/push**: GitHub Actions `ci.yml` — spotless + test + Docker build

- **배포**: Jenkins (`Jenkinsfile`) — EC2 docker compose

- **릴리즈 태그**: `release` 브랜치 push → `release-tag.yml`



## 관측성



| 계층 | 도구 | 용도 |

|------|------|------|

| 메트릭 | Prometheus + Grafana `burty-overview` | HTTP, JVM, Circuit Breaker |

| **로그** | **Loki + Promtail + Grafana `burty-logs`** | 앱 로그 검색, requestId 추적 |

| 트레이싱 | OTel Collector | 분산 트레이스 (선택) |



외부 인터넷에서는 nginx가 `/actuator/` 를 404로 차단합니다.



### Grafana 임베딩 (ADMIN API)



ADMIN JWT + `X-Risk-Proof` (LEVEL_2) 로 호출:



```http

GET /api/v1/admin/observability/dashboards

GET /api/v1/admin/observability/embed/dashboard?uid=burty-overview

GET /api/v1/admin/observability/embed/dashboard?uid=burty-logs

```



| env | 용도 |

|-----|------|

| `BURTY_GRAFANA_ENABLED=true` | 임베딩 API 활성화 |

| `GRAFANA_PUBLIC_URL` | 브라우저가 접근하는 Grafana URL |

| `HOST_LOG_DIR` | Promtail이 읽는 로그 디렉터리 (burty와 동일 경로) |



## prod 필수 env



| 변수 | 용도 |

|------|------|

| `JWT_SECRET`, `BURTY_SIGN_SECRET`, `ADMIN_SETUP_KEY` | 인증/서명 |

| `BURTY_REDIS_ENABLED=true` | JWT blacklist, rate limit, 큐 |

| `BURTY_FIELD_ENCRYPTION_KEY` | 마이데이터 토큰 암호화 |

| `MYDATA_*`, `OB_*` | 금융 API 키 |

| `MAIL_*` | 이메일 알림 (stub-mode=false 시) |



`ProdStartupValidator`가 prod에서 stub/기본 secret 미설정 시 기동을 차단합니다.



## 장애 대응



1. **502 from nginx**: `docker logs burty`, `curl http://burty:8080/health`

2. **로그가 Grafana에 안 보임**: `docker logs burty-promtail`, `./logs/application.log` 존재 여부 확인

3. **Redis down**: JWT blacklist in-memory fallback — 단일 인스턴스만 안전

4. **외부 API 장애**: Resilience4j circuit breaker `openbanking`/`mydata` OPEN

5. **배치 실패**: ShedLock — `tbl_shedlock` 확인


