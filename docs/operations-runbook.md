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



# 백업과 복구

> **복구되지 않는 백업은 백업이 아니다.** 백업이 도는 것과 복구가 되는 것은 다른 문제다.

## 스크립트

| 스크립트 | 역할 |
|---|---|
| `backup-mariadb.sh` | 논리 백업 + 무결성 검증 + 메타(체크섬·binlog 위치) 기록 |
| `restore-mariadb.sh` | 복구 + 복구 결과 검증 |
| `backup-restore-rehearsal.sh` | 백업→복구→검증을 한 번에 (리허설 자동화) |

## 정기 리허설

월 1회 이상 실행한다. 사람이 기억해서 하는 절차는 결국 하지 않게 되므로 cron 에 건다.

```bash
DB_HOST=... DB_NAME=burty DB_USER=... DB_PASSWORD=... \
  ./infra/scripts/backup-restore-rehearsal.sh
```

운영 DB 는 읽기만 한다. 복구는 `burty_rehearsal` 로 하고 끝나면 지운다.
출력의 **복구 소요 시간이 RTO 추정치**다. RPO 는 백업 cron 주기로 결정된다.

리허설이 실패하면 그 자체가 장애다. 백업이 쓸모없는 상태라는 뜻이므로 즉시 원인을 찾는다.

## 실제 복구

```bash
# 1. 별도 DB 로 먼저 복구해 내용을 확인한다
DB_HOST=... DB_USER=... DB_PASSWORD=... \
  ./infra/scripts/restore-mariadb.sh ./backups/burty_burty_20260828_030000.sql.gz burty_restore

# 2. 애플리케이션을 복구본으로 띄워 ddl-auto=validate 통과를 확인한다
# 3. 감사 체인 검증을 수동 실행한다
# 4. 문제 없으면 서비스를 복구본으로 전환하거나 운영 DB 에 반영한다
```

운영 DB 를 직접 덮어쓰려면 대상 이름을 명시하고 `CONFIRM=yes` 를 줘야 한다.
사고로 일어나기 쉬운 조작이라 일부러 번거롭게 만들었다.

## 복구 후 반드시 확인할 것

- **Flyway 이력** — `flyway_schema_history` 가 비어 있으면 애플리케이션이 마이그레이션을
  다시 적용하려 든다. 복구 스크립트가 이를 검사한다.
- **감사 로그 해시 체인** — 부분 복구나 행 유실이 있으면 체인이 끊긴다. 복구 스크립트가
  순번 불연속을 세고, 감사 검증 배치로 전체를 확인한다.
- **미결 이체** — 복구 시점 이후 실제로는 은행에서 처리된 건이 있을 수 있다.
  `GET /api/v1/admin/ops/transfers/pending-reconciliation` 로 확인하고 원장과 대조한다.

## 백업 스크립트가 검증하는 것

단순히 덤프를 뜨고 끝내지 않는다. 검증 없는 백업은 복구를 시도하기 전까지 문제를 알 수 없다.

- gzip 스트림 무결성
- 최소 크기 (빈 덤프 방지)
- 핵심 테이블 존재 (`tbl_user`, `tbl_transfer_order`, `flyway_schema_history`)
- SHA-256 체크섬을 메타 파일에 기록 → 복구 시 대조
- 바이너리 로그 위치 기록 → 시점 복구(PITR) 기준점

정리는 **새 백업이 검증을 통과한 뒤에만** 수행한다. 백업이 며칠 멈춰 있었는데 정리만
돌면 전부 사라진다.

## DB 백업



```bash

DB_HOST=localhost DB_PASSWORD=... ./infra/scripts/backup-mariadb.sh

```



- 기본 보관: 14일 (`BACKUP_DIR`, `RETENTION_DAYS` 환경변수로 변경)

- 복구: `gunzip -c backups/burty_*.sql.gz | mysql -u root -p burty`



## CI/CD



- **PR/push**: GitHub Actions `ci.yml` — spotless + test + Trivy + K8s 매니페스트 검증(kustomize build + kubeconform)

- **빌드/승격**: Jenkins (`Jenkinsfile`) — 이미지 빌드(kaniko) → Trivy 스캔 → `k8s/overlays/<env>` 에 이미지 태그 커밋

- **배포**: ArgoCD — 커밋을 클러스터에 반영. Jenkins 는 배포하지 않는다.

- **릴리즈 태그**: `release` 브랜치 push → `release-tag.yml`

- 이전 compose 배포 파이프라인은 `Jenkinsfile.compose` 로 보존되어 있다.



## 관측성



| 계층 | 도구 | 용도 |

|------|------|------|

| 메트릭 | Prometheus + Grafana `burty-overview` | HTTP, JVM, Circuit Breaker |

| **로그** | **Loki + Promtail + Grafana `burty-logs`** | 앱 로그 검색, requestId 추적 |

| 트레이싱 | OTel Collector | 분산 트레이스 (선택) |



외부 인터넷에서는 Istio VirtualService 의 `directResponse` 가 `/actuator/` 를 404로 차단합니다 (compose 환경에서는 nginx). 메시 내부에서도 `AuthorizationPolicy: deny-actuator-from-mesh` 로 막혀 있으며, Prometheus 는 8080 이 아니라 istio-proxy 의 병합 메트릭 포트 15020 을 긁습니다.



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



## 스테이징

목적은 "기능이 되나" 가 아니라 **"연동 경로가 실제로 도나"** 다.

지금까지 모든 외부 연동이 stub 이었다. stub 은 어댑터가 HTTP 를 아예 타지 않고 가짜
객체를 돌려주므로 **직렬화·타임아웃·에러 매핑·서킷브레이커가 전부 검증되지 않는다.**
운영에서 문제가 되는 것은 대부분 그 경로다.

스테이징은 WireMock 을 향해 실제 HTTP 를 낸다. 금융기관 자격증명은 필요 없다.

```bash
docker compose -f docker-compose.staging.yml up -d --build
./infra/staging/seed.sh     # 거래 5만 건
./infra/staging/smoke.sh    # 연동이 실제 HTTP 를 타는지 확인
```

`smoke.sh` 는 WireMock 의 요청 카운터가 늘어나는지로 판정한다. 늘지 않으면 stub 으로
돌고 있다는 뜻이므로 실패로 처리한다.

### 실패 시나리오 재현

목의 응답은 **금액**으로 고른다. 헤더는 앱을 거쳐 은행으로 전달되지 않기 때문이다.

| 금액 | 목 응답 | 기대 동작 |
|---|---|---|
| 99999 | 30초 지연 | 주문이 `UNKNOWN` 으로 남고 정산 대상이 된다. **한도를 되돌리지 않는다.** |
| 88888 | 503 | 위와 같다 (처리 중이었을 수 있다) |
| 77777 | 400 | 확정 실패. 출금이 없었으므로 한도를 되돌린다. |
| 그 외 | 200 | 정상 완료 |

```bash
./infra/staging/transfer-scenario.sh
```

**전제조건** — 사용자에게 오픈뱅킹 연동 기관이 등록돼 있어야 한다. 없으면 이체가
404(미연동)로 거절되고 아무것도 확인하지 못한다. 연동은 OAuth 링크 흐름을 거쳐야
만들어진다 (SQL 시딩으로는 `access_token` 암호화 때문에 안 된다).

이 전제 때문에 CI 스모크에는 이 단계가 빠져 있다. 링크 흐름을 목으로 세우는 것이
다음 단계다.

`GET /api/v1/admin/transfers/pending-reconciliation` 으로 정산 대기 건을 확인한다.

### 부하 시험

레이트리밋을 끄고 재기동해야 처리량을 잴 수 있다. 켜둔 채로 측정하면 애플리케이션이
아니라 레이트리밋 설정을 재게 된다.

```bash
BURTY_API_RATELIMIT_ENABLED=false docker compose -f docker-compose.staging.yml up -d burty
k6 run -e BASE_URL=http://localhost:8080 -e TOKEN=$TOKEN -e PROFILE=full \
  infra/loadtest/burty-load.js
```

측정값은 `infra/loadtest/README.md` 의 표에 근거와 함께 채운다.

### 스테이징에서 하지 않는 것

알림(이메일·SMS·푸시)과 소셜 로그인은 stub 으로 둔다. 실제 발송은 사람에게 도달한다.

## prod 필수 env

`ProdStartupValidator` 가 아래 조건을 하나라도 어기면 **기동을 중단한다.** 검증은 웹 서버가
포트를 열기 전에 돌기 때문에, 잘못된 설정으로 잠깐이라도 트래픽을 받는 일은 없다.

배포가 `PROD startup blocked: ...` 로 실패하면 그 메시지의 설정값을 아래에서 찾으면 된다.

### 시크릿 (기본값이면 차단)

| 변수 | 용도 |
|---|---|
| `JWT_SECRET` | 액세스 토큰 서명 |
| `BURTY_SIGN_SECRET` | 요청 서명 |
| `ADMIN_SETUP_KEY` | 관리자 부트스트랩 키 |
| `BURTY_FIELD_ENCRYPTION_KEY` | 마이데이터 토큰 등 필드 암호화 |

### 인프라

| 변수 | 값 | 왜 |
|---|---|---|
| `BURTY_REDIS_ENABLED` | `true` | JWT 블랙리스트·레이트리밋·큐가 인스턴스 간 공유돼야 한다 |
| `TRUSTED_PROXIES` | 인그레스/LB 의 CIDR | 전달 헤더를 신뢰할 범위. **비워두면 기동하지 않는다** |

`TRUSTED_PROXIES` 를 비워두면 모든 요청이 프록시 IP 하나로 기록되어 레이트리밋이 전체
트래픽을 한 버킷에 몰아넣고 접근 기록도 무의미해진다. 그래서 조용히 그렇게 되도록 두지 않는다.
반대로 너무 넓게 잡으면 클라이언트가 자기 IP 를 지정할 수 있게 되므로 실제 프록시 주소로 좁힌다.

### stub 은 전부 꺼야 한다

`burty.mydata`, `burty.social`, `burty.external`, `burty.identity`, `burty.webauthn`,
`burty.notify.email`, `burty.notify.sms`, `burty.notify.push` 의 `stub-mode` 가 하나라도
`true` 면 차단된다.

특히 `burty.webauthn.stub-mode` 는 **서명을 검증하지 않는** 스텁이라, 켜져 있으면 이체의
생체인증 게이트가 무력화된다.

### 그 밖에 차단되는 설정

| 설정 | 요구 | 왜 |
|---|---|---|
| `burty.admin.bootstrap-enabled` | `false` | 무인증 관리자 등록 창구 |
| `burty.api.swagger-enabled` | `false` | 내부 API 명세 노출 |
| 본인확인 자격증명 | NICE 또는 KCB 설정 완료 | provider 를 지정했으면 자격증명이 있어야 한다 |
| `MAIL_HOST`, SMS 자격증명, FCM 자격증명 | 설정 | 해당 채널 stub 을 껐다면 실제 연동이 있어야 한다 |

### 외부 연동 키

`MYDATA_*`, `OB_*`, `MAIL_*` — 금융 API 및 알림 채널.

## 장애 대응



1. **502 from nginx**: `docker logs burty`, `curl http://burty:8080/health`

2. **로그가 Grafana에 안 보임**: `docker logs burty-promtail`, `./logs/application.log` 존재 여부 확인

3. **Redis down**: JWT blacklist in-memory fallback — 단일 인스턴스만 안전

4. **외부 API 장애**: Resilience4j circuit breaker `openbanking`/`mydata` OPEN

5. **배치 실패**: ShedLock — `tbl_shedlock` 확인

## Kubernetes 운영 (prod/dev)



매니페스트와 설치 순서는 `k8s/README.md` 를 본다. 여기에는 당직이 실제로 치는 명령만 둔다.



### 어디에 무엇이 있나



| 대상 | 위치 |

|---|---|

| 앱 | `burty-prod` / `burty-dev` 네임스페이스, Deployment `burty-api` |

| 인그레스 | `istio-system` 의 `istio-ingressgateway`, Gateway `burty-gateway` |

| 관측 | `observability` — Prometheus / Grafana / Loki / Tempo / OTel Collector |

| CD | `argocd` — Application `burty-prod`, `burty-dev`, `burty-platform` |

| CI | `jenkins` |



### 상태 확인



```bash

kubectl -n burty-prod get pods,hpa,pdb

kubectl -n burty-prod logs -l app.kubernetes.io/name=burty-api --tail=100 -c burty-api

argocd app get burty-prod

istioctl proxy-status

```



### 배포 / 롤백



배포의 진실 공급원은 Git 이다. 클러스터를 직접 고치면 ArgoCD 가 되돌린다(dev 는 selfHeal 켜짐).



```bash

# 정상 배포 — Jenkins 파이프라인이 이미지 태그를 커밋한 뒤 승인 단계에서 대기한다

argocd app sync burty-prod && argocd app wait burty-prod --health --sync



# 즉시 롤백 (직전 리비전으로)

argocd app history burty-prod

argocd app rollback burty-prod <REVISION>



# 항구적 롤백 — Git 이 진실이므로 이쪽이 정석

git revert <이미지-태그-커밋> && git push

```



`kubectl rollout undo` 는 쓰지 않는다. ArgoCD 가 다음 sync 에서 되돌려 놓는다.



### 긴급 스케일



```bash

kubectl -n burty-prod scale deploy/burty-api --replicas=6

```



HPA 가 소유한 값이라 곧 되돌아간다. 지속시키려면 `k8s/base/hpa.yaml` 의 `minReplicas` 를 올려 커밋한다.



### 장애 대응 — K8s 편



1. **502/503 from gateway**

   `istioctl proxy-config route deploy/istio-ingressgateway.istio-system` 으로 라우트 순서를 본다.

   VirtualService 는 nginx 와 달리 **위에서 첫 매치**라, 규칙 순서가 바뀌면 조용히 오라우팅된다.



2. **파드가 Running 인데 트래픽이 안 감**

   `AuthorizationPolicy: allow-nothing` 이 네임스페이스 기본 거부다.

   새 워크로드를 올렸다면 그 워크로드용 ALLOW 정책이 있는지 먼저 본다.

   `istioctl analyze -n burty-prod` 가 대부분 잡아 준다.



3. **파드가 CreateContainerConfigError**

   대개 `burty-secret` 미존재다. ESO 를 쓴다면 `kubectl -n burty-prod get externalsecret` 의 상태를 본다.



4. **메트릭이 Grafana 에 안 보임**

   mTLS STRICT 라 8080 직접 스크랩은 실패한다. 파드에 `prometheus.io/*` 애노테이션이 있는지,

   istiod values 의 `enablePrometheusMerge: true` 인지, PodMonitor 가 15020 을 보는지 순서대로 확인한다.



5. **로그가 Loki 에 안 보임**

   K8s 에서는 파일이 아니라 **stdout** 을 수집한다. `kubectl -n observability logs -l app=promtail` 확인.

   컨테이너 안 `/app/logs` 는 emptyDir 이며 수집 대상이 아니다.



6. **사이드카 없이 뜬 파드** (`BurtySidecarMissing` 알람)

   네임스페이스의 `istio-injection=enabled` 라벨이 빠졌거나, PSA 를 `restricted` 로 올려

   `istio-init` 이 거부된 경우다. prod 네임스페이스는 `baseline` 이어야 한다.



7. **인증서 만료 임박/실패** (`BurtyTlsCertExpiringSoon`, `BurtyCertNotReady`)

   ```bash

   kubectl -n istio-system get certificate,certificaterequest,order,challenge

   kubectl -n cert-manager logs deploy/cert-manager --tail=100

   ```



8. **HPA 가 최대치에 붙어 내려오지 않음**

   CPU 만 보게 되어 있다. 메모리 메트릭을 다시 넣지 말 것 — JVM 은 힙을 반환하지 않아

   requests 기준 utilization 이 항상 100% 를 넘어 영구 고정된다.



### DB 백업 — prod



1차는 RDS 자동 백업(PITR)이다. 2차 논리 덤프는 CronJob `burty-db-backup` 이 매일 03:20 KST 에

S3 로 올린다.



```bash

kubectl -n burty-prod get cronjob burty-db-backup

kubectl -n burty-prod create job --from=cronjob/burty-db-backup manual-$(date +%s)   # 수동 실행



# 복구

aws s3 cp s3://<버킷>/mariadb/burty_burty_YYYYMMDD_HHMMSS.sql.gz .

gunzip -c burty_*.sql.gz | mariadb -h <RDS> -u <user> -p burty

```



보존은 S3 Lifecycle 로 건다(스크립트의 `RETENTION_DAYS` 대체).


---

# 알람별 조치 절차

각 알람에는 대응하는 조치 API가 있다. 알람만 있고 조치 수단이 없으면 알람은 소음이 된다.

## TransferResultUnknown (critical)

**의미**: 정산 배치가 재시도를 소진했다. 은행 응답을 끝내 확인하지 못한 이체가 있다는 뜻이며, **실제로 출금됐는지 알 수 없는 상태**다.

**조치**

1. 미확정 건 조회
   ```http
   GET /api/v1/admin/ops/transfers/pending-reconciliation?limit=100
   ```
2. 각 건의 `idempotencyKey` 로 은행 원장을 대조한다. (오픈뱅킹 거래조회 또는 기관 담당자 문의)
3. 대조 결과를 확정한다. **근거 없이 확정할 수 없다.**
   ```http
   POST /api/v1/admin/ops/transfers/{orderId}/confirm
   { "executed": true,  "evidence": "은행거래번호 B1A2C3..." }   # 실제 출금됨
   { "executed": false, "evidence": "2026-08-27 원장 대조, 해당 건 없음" }  # 미출금
   ```

`executed=false` 인 경우에만 일일 이체 한도가 복구된다. 출금된 건의 한도를 되돌리면 사용자가 한도를 두 번 쓰게 되므로, **확실하지 않으면 확정하지 말 것.**

모든 확정 호출은 감사 로그에 조작자와 근거가 함께 남는다.

## OutboxDeadLetter (critical)

**의미**: 이벤트가 재시도를 소진해 격리됐다. 알림·보호자 승인 요청 등이 **발송되지 않은 상태**다.

**조치**

1. 격리된 이벤트와 실패 원인 확인
   ```http
   GET /api/v1/admin/ops/outbox/dead?limit=100
   ```
2. `lastError` 로 원인을 파악하고 **먼저 고친다.** (외부 채널 장애, 설정 오류, 처리기 버그 등)
3. 원인 해소 후 재처리
   ```http
   POST /api/v1/admin/ops/outbox/redrive
   { "eventIds": [1234, 1235] }
   ```

원인을 고치지 않고 재처리하면 다시 DEAD 로 간다. 재처리는 시도 횟수를 0 으로 되돌려 재시도 예산을 새로 준다.

## CircuitBreakerOpen (critical)

**의미**: 외부 연동(오픈뱅킹/마이데이터)이 연속 실패해 차단됐다. 해당 기능이 중단된 상태다.

**조치**: 기관 상태 페이지 확인 → 자체 문제면 `resilience4j.circuitbreaker` 설정과 토큰 유효성 확인. 차단 중 발생한 이체는 `UNKNOWN` 으로 쌓이므로 복구 후 정산 배치가 정리한다.

## MyDataTokenRefreshStalled (warning)

**의미**: 토큰 갱신 배치가 1시간 이상 성공하지 못했다. 방치하면 마이데이터 연동이 **조용히 끊긴다.**

**조치**: 애플리케이션 로그에서 `MyDataTokenRefreshBatch` 확인. ShedLock 락이 걸린 채 남아 있는지도 확인 (`SELECT * FROM shedlock WHERE name='MyDataTokenRefreshBatch'`).

## DbConnectionPoolExhausted (critical)

**의미**: 커넥션 대기가 발생했다. 커넥션을 오래 잡는 코드가 있다는 신호다.

**조치**: `leak-detection-threshold=20000` 이 켜져 있으므로 로그에서 누수 경고를 찾는다. 외부 HTTP 호출이 트랜잭션 안에 들어간 코드가 새로 추가됐는지 확인 (이체 경로는 의도적으로 트랜잭션 밖에서 호출한다).

---

# 정기 점검

| 주기 | 항목 |
|---|---|
| 매일 | 감사 체인 검증 결과 (`감사 로그 체인 검증 통과` 로그), DEAD 이벤트 수 |
| 주간 | 정산 미확정 건 잔량, 마이데이터 동의 만료 예정 건 |
| 배포 시 | Flyway 마이그레이션 적용 로그, `ddl-auto=validate` 통과 여부 |

---

# 스키마 변경 절차

1. 엔티티를 수정한다.
2. **새 마이그레이션 파일**을 추가한다 (`V{n}__설명.sql`). 기존 파일은 절대 수정하지 않는다 — Flyway 가 체크섬을 검증하므로 기존 환경이 깨진다.
3. `./gradlew check` 로 검증한다. CI 에서 실제 MariaDB 에 적용되어 엔티티와 대조된다.
4. enum 상수를 추가했다면 `EnumColumnWidthTests` 가 컬럼 길이를 확인한다. 초과하면 마이그레이션으로 넓힌다.

베이스라인(`V3`)을 재생성해야 하는 경우는 **아직 어디에도 적용되지 않았을 때뿐**이다.

---

# 로그 개인정보 정책

로그는 Loki 에 수집되어 보존된다. 계좌번호·전화번호가 평문으로 쌓이면 **로그 자체가 개인정보
처리 대상**이 되므로, 두 계층으로 막는다.

## 1. 호출부 명시 마스킹 (원칙)

```java
log.info("이체 완료 account={}", PiiMasker.account(accountNo));
```

| 대상 | 메서드 | 결과 |
|---|---|---|
| 계좌번호·핀테크이용번호 | `PiiMasker.account()` | `***7890` |
| 전화번호 | `PiiMasker.phone()` | `***5678` |
| 이름 | `PiiMasker.name()` | `홍***` |
| 이메일 | `PiiMasker.email()` | `ro***@example.com` |
| 토큰·비밀값 | `PiiMasker.secret()` | `***(len=124)` |

## 2. logback 스크럽 (안전망)

`%maskedMsg` / `%maskedEx` 변환기가 출력 직전에 한 번 더 훑는다.
주민번호·전화번호·계좌번호·JWT·`token=...` 형태를 정규식으로 가린다.

이 계층이 필요한 이유는 **예외 메시지를 우리가 통제할 수 없기 때문**이다.
DB 제약 위반 메시지에는 위반한 컬럼 값이, 외부 API 오류에는 응답 본문이 그대로 들어온다.

> 정규식은 완벽하지 않다. 1번을 대체하지 않는다.

## 로그를 추가할 때

- **값을 찍기 전에 "이게 유출되면 곤란한가" 를 먼저 물을 것.** 곤란하면 `PiiMasker` 를 쓴다.
- 요청 객체·엔티티·`Map` 을 통째로 찍지 말 것. 필요한 필드만 골라 찍는다.
- 알림 본문(`body`)에는 금액·계좌가 들어간다. 제목까지만 남긴다.
- `LoggingAspect` 는 인자 **값** 이 아니라 **타입** 만 남긴다. 값이 필요하면 호출부에서 직접 남긴다.

## 점검

```bash
# 마스킹 없이 민감 파라미터를 찍는 로그 찾기
grep -rn "log\.\(info\|warn\|error\|debug\)" src/main \
  | grep -iE "\b(fromAccount|toAccount|accountNo|fintechUseNum|phone|ci|birthdate|token)\b" \
  | grep -v PiiMasker
```

`PiiMaskerTests` 와 `LogbackPiiMaskingTests` 가 마스킹 로직과 배선을 각각 검증한다.

---

# 필드 암호화 키 로테이션

마이데이터 토큰은 `FieldEncryptor` 로 암호화되어 `tbl_linked_institution` 에 저장된다.
암호문 앞 1바이트가 **키 버전**이라 어느 키로 썼는지 암호문만 보고 알 수 있다.

> **순서를 지킬 것.** 구키 설정을 먼저 지우면 기존 토큰이 전부 복호화 불가가 되고,
> 되돌릴 방법이 없다. 사용자 연동이 통째로 끊긴다.

## 1. 신키 투입 (구키는 남겨둔다)

```bash
BURTY_FIELD_ENCRYPTION_KEY=<신키>
BURTY_FIELD_ENCRYPTION_KEY_VERSION=3          # 기존이 2 였다면 3

BURTY_FIELD_ENCRYPTION_PREVIOUS_KEY=<구키>
BURTY_FIELD_ENCRYPTION_PREVIOUS_KEY_VERSION=2
```

배포하면 이 시점부터 **새로 쓰는 값은 v3**, 기존 v2 값은 계속 읽힌다.
기동 로그에 `필드 암호화 키 로테이션 활성 — 쓰기 v3, 복호화 호환 [3, 1, 2]` 가 나오는지 확인한다.

## 2. 재암호화 배치 켜기

```bash
BURTY_FIELD_ENCRYPTION_ROTATION=true
```

매일 03:15 에 구키로 쓰인 행을 찾아 신키로 다시 쓴다. 한 주기 상한은 2,000행이므로
데이터가 많으면 며칠 걸린다. 진행 상황은 로그와 메트릭으로 본다.

```
필드 암호화 로테이션 — 검사 2000 / 재암호화 1840 / 실패 0
burty_encryption_rotation_total{outcome="rotated"}
burty_encryption_rotation_total{outcome="failed"}
```

**실패가 0 이 아니면 멈추고 원인부터 확인한다.** 복호화 실패는 키 설정 오류일 가능성이
높고, 그 상태로 계속 돌리면 실패만 쌓인다.

## 3. 완료 확인 후 구키 제거

`재암호화 0 / 실패 0` 이 며칠 연속 나오면 남은 구키 데이터가 없다는 뜻이다.

```sql
-- 남은 구버전 암호문 확인 (v3 = 0x03 → base64 첫 글자가 'A' 로 시작하지 않는 것)
SELECT COUNT(*) FROM tbl_linked_institution
WHERE access_token IS NOT NULL AND status = 'ACTIVE';
```

확인 후 구키 설정과 배치를 제거한다.

```bash
BURTY_FIELD_ENCRYPTION_PREVIOUS_KEY=          # 비움
BURTY_FIELD_ENCRYPTION_PREVIOUS_KEY_VERSION=0
BURTY_FIELD_ENCRYPTION_ROTATION=false
```

## 주의

- **현재 키와 이전 키의 버전을 같게 두지 말 것.** 구분이 불가능해진다. 애플리케이션이
  기동 시점에 거부한다.
- Redis·인메모리 토큰 스토어는 TTL 로 자연 교체되므로 로테이션 대상이 아니다.
- 재암호화는 멱등하다. 중간에 멈춰도 다음 주기에 이어서 하면 된다.
