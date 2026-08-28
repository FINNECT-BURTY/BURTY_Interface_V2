# 부하 시험

## 목적

"몇 RPS 를 버티나"가 아니다. 현재 설정값은 **합리적 기본값일 뿐 실측이 아니다.**
무엇이 먼저 무너지는지 찾아 그 값을 조정하는 것이 목적이다.

| 값 | 현재 | 근거 |
|---|---|---|
| HikariCP 최대 커넥션 | 20 | 기본값 |
| 스케줄러 풀 | 8 | 배치 5 + 큐 폴러 + 여유 |
| 비동기 풀 | 8~32 | 기본값 |
| 외부 API 타임아웃 | 연동별 5~15초 | 상대 SLA 추정 |

## 실행

```bash
# 1. 토큰 발급 (dev 프로파일)
TOKEN=$(curl -s -XPOST localhost:8080/api/v1/auth/token \
  -H 'Content-Type: application/json' -d '{"userId":"1"}' | jq -r .data.token)

# 2. 시험 실행
k6 run -e BASE_URL=http://localhost:8080 -e TOKEN="$TOKEN" infra/loadtest/burty-load.js
```

> **stub-mode 가 켜진 환경에서만 실행할 것.** 이체 시나리오는 실제 이체 경로를 탄다.
> 운영을 대상으로 돌리지 말 것.

## 시나리오

- **조회** — 30 VU 유지 후 80 VU 로 상승. 대부분의 트래픽이 조회이므로 여기서 먼저 무너진다.
- **이체** — 초당 5건 고정. 빈도는 낮지만 가장 비싼 경로다(은행 호출 + 트랜잭션 + 아웃박스).

멱등키는 매 요청마다 새로 만든다. 같은 키를 재사용하면 두 번째부터 재요청으로 처리되어
실제 이체 경로를 타지 않는다.

## 함께 볼 지표

k6 결과만 보면 원인을 알 수 없다. Grafana 에서 아래를 동시에 본다.

| 지표 | 무엇을 뜻하나 |
|---|---|
| `hikaricp_connections_pending` | 0 이 아니면 **커넥션 풀이 병목**이다 |
| `hikaricp_connections_active` | 20 에 붙어 있으면 포화 |
| `resilience4j_circuitbreaker_state` | 외부 연동이 차단됐는지 |
| `burty_outbox_dead_total` | 아웃박스가 적체·격리되는지 |
| `jvm_memory_used_bytes` | 힙 압박 |

## 결과를 어디에 반영하나

- 커넥션 대기가 발생하면 → `DB_POOL_MAX` 상향, **또는** 커넥션을 오래 잡는 코드를 찾는다.
  이체 경로는 의도적으로 외부 호출을 트랜잭션 밖에서 한다. 새로 추가된 코드가 이를
  어겼는지 `leak-detection-threshold` 경고로 확인한다.
- 이체 p95 가 튀면 → 은행 API 타임아웃과 서킷브레이커 임계값을 재조정한다.
- 조회 p95 가 튀면 → 캐시 적중률과 N+1 을 확인한다.

측정한 값은 이 문서의 표에 근거와 함께 갱신한다. **"기본값"이 아니라 "실측 후 이 값"이
되어야 한다.**
