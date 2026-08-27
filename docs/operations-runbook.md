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
