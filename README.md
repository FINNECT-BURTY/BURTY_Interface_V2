# BURTY

금융 마이데이터와 오픈뱅킹을 기반으로 한 개인 자산관리 서비스. 시니어 사용자와 보호자를 함께 고려한 가족 보호 기능이 핵심 축이다.

| | |
|---|---|
| **스택** | Java 21 · Spring Boot 4 · MariaDB · Redis · Gradle |
| **아키텍처** | 헥사고날 (adapter.in / application.port / application.service / domain / adapter.out) |
| **규모** | 프로덕션 약 640 파일 · 엔드포인트 150+ · 엔티티 50 |
| **관측성** | Prometheus · Loki · Grafana · OpenTelemetry |

---

## 이 프로젝트에서 다룬 문제

가계부 앱이 아니라 **돈을 실제로 움직이는 시스템**이다. 그래서 기능 목록보다 아래 질문들에 어떻게 답했는지가 중요하다.

> 은행에 이체를 요청했는데 응답이 오지 않으면, 그 건은 성공인가 실패인가?

정답은 "둘 다 아니다"이다. 이 구분을 코드로 표현하는 것이 이 프로젝트의 중심 주제다.

### 1. 이체 결과가 불확실할 때

타임아웃이나 5xx는 **요청이 은행에 도달했는지조차 알 수 없는** 상태다. 이걸 실패로 확정하면, 실제로는 출금된 건을 사용자가 재시도해 이중 출금이 발생한다.

```
성공          → EXECUTED
명확한 거절    → FAILED   + 한도 복구
응답 확인 불가 → UNKNOWN  + 한도 유지 + 정산 대상 등록
```

`UNKNOWN` 건은 [`TransferReconciliationBatch`](src/main/java/com/burty/application/service/batch/TransferReconciliationBatch.java)가 은행에 재조회해 확정한다. **미출금이 확인됐을 때만** 한도를 되돌린다. 자동으로 풀리지 않는 건은 [운영 API](src/main/java/com/burty/adapter/in/web/admin/OperationsAdminController.java)로 담당자가 은행 원장과 대조해 확정하며, 그 행위 자체가 감사 로그에 남는다.

### 2. 외부 호출을 트랜잭션에 넣지 않기

[`TransferService`](src/main/java/com/burty/application/service/finance/TransferService.java)에는 `@Transactional`이 없다. 의도적이다. 은행 호출을 DB 트랜잭션 안에 넣으면

- 네트워크 왕복 내내 커넥션을 점유하고,
- 실패 시 롤백되어 **실패 기록조차 남지 않으며**,
- 은행 호출 성공 후 커밋이 실패하면 돈은 나갔는데 기록이 없다.

상태 전이는 [`TransferOrderWriter`](src/main/java/com/burty/application/service/finance/TransferOrderWriter.java)가 `REQUIRES_NEW`로 독립 커밋하고, 오케스트레이터는 순서만 조율한다.

### 3. 멱등성은 DB가 판정한다

"조회해서 없으면 실행"은 멱등이 아니다. 동시 요청 둘이 모두 조회에서 empty를 받으면 둘 다 실행된다. 대신 `(user_id, idempotency_key)` 유니크 제약 위반을 신호로 삼아 **INSERT로 선점**한다. 애플리케이션 레벨 경쟁 구간이 없다.

은행에 보내는 거래고유번호도 멱등키에서 결정론적으로 도출한다. 재시도마다 난수를 새로 만들면 은행 쪽 중복 방지가 무력해진다.

### 4. 한도 검사에서 배운 것

일일 이체 한도는 세 번 갈아엎었다.

| 시도 | 결과 |
|---|---|
| 조건부 `UPDATE ... WHERE total + ? <= limit` | 동시성 테스트에서 한도 10건에 **14건 통과** |
| `SELECT ... FOR UPDATE` | 여전히 12건 통과 (엔진 MVCC 구현 의존) |
| **`@Version` 낙관적 잠금 + 트랜잭션 밖 재시도** | 통과 |

JPA의 `@Version`은 UPDATE의 WHERE에 버전을 넣고 영향 행 수를 확인하는 방식이라, DB 엔진의 격리 수준 구현과 무관하게 lost update를 잡아낸다. **돈이 걸린 검사를 엔진 동작 차이에 맡길 수 없다**는 것이 이 과정에서 얻은 결론이다.

### 5. 부수효과는 아웃박스로

이체 완료 알림을 커밋 시점에 직접 보내면, 커밋은 됐는데 발송이 실패하거나(유실) 발송은 됐는데 롤백되는(유령 알림) dual-write 문제가 생긴다.

[`OutboxPublisher`](src/main/java/com/burty/application/port/out/outbox/OutboxPublisher.java)는 `MANDATORY` 전파로 선언되어 **트랜잭션 없이 호출하면 예외**가 난다. 원자성 보장이 조용히 깨지는 일이 없다. [`OutboxRelay`](src/main/java/com/burty/application/service/outbox/OutboxRelay.java)가 커밋 이후 꺼내 발송하고, 실패는 지수 백오프로 재시도하다가 소진되면 `DEAD`로 격리한다. 조용히 ACK하고 버리지 않는다.

### 6. 사후 통지가 아니라 차단

가족 보호 기능에서 "이상 이체를 알린다"는 것만으로는 피해를 막지 못한다. 알림이 갔을 때는 이미 출금이 끝난 뒤다.

`VIEW_ALERT_AND_APPROVE` 권한을 가진 보호자가 있으면, 임계 금액 이상의 이체는 **실행되지 않고 보류**된다. 보호자가 승인해야 실행되고, 거절하거나 기한이 지나면 취소된다. 승인은 "실행해도 좋다"는 허가일 뿐 피보호자의 생체인증을 대체하지 않는다.

### 7. 개인정보는 즉시 파기와 법정 보존으로 나뉜다

탈퇴 시 "전부 즉시 삭제"는 전자금융거래법 위반이고, "아무것도 안 지움"은 개인정보보호법 위반이다. 어느 쪽인지 코드가 명시적으로 말해야 한다.

| 즉시 | 보존 후 파기 |
|---|---|
| CI·전화번호·이름·생년월일 익명화 | 전자금융거래 기록 (기본 5년) |
| 세션·기기·생체인증·소셜 연동 폐기 | 감사 로그 (해시 체인 무결성 유지) |
| 마이데이터 수집 계좌 데이터 파기 | |

[`tbl_data_erasure_request`](src/main/java/com/burty/domain/user/entity/DataErasureRequestEntity.java)가 "언제 무엇을 지웠고 무엇이 언제까지 남는지"를 기록하며, 이 기록 자체가 처리 증빙이 된다.

### 8. 감사 로그는 지울 수 있으면 감사 로그가 아니다

각 행이 직전 행의 SHA-256 해시를 품는다. 중간 행을 고치거나 지우면 이후 체인이 전부 어긋나 [일일 검증](src/main/java/com/burty/application/service/admin/AuditChainVerifier.java)에서 드러난다.

---

## 실행

```bash
# 로컬 전체 스택 (앱 + MariaDB + Redis + 관측성)
docker network create global-nginx 2>/dev/null || true
cp .env.example .env      # 값 채우기
docker compose -f docker-compose.yml -f docker-compose.local.yml up -d --build
```

| | |
|---|---|
| API | http://localhost:8080/health |
| Swagger | http://localhost:8080/api/v1/swagger-ui.html (dev만) |
| Grafana | http://localhost:3001 |
| Prometheus | http://localhost:9090 |

```bash
./gradlew check     # Spotless + 테스트 + 커버리지 게이트
./gradlew test      # 테스트만
```

### 스키마

Flyway로 관리한다. `V3__fresh_install_baseline.sql`이 전체 스키마를 만들고 이후 마이그레이션이 변경을 쌓는다. 모든 문장이 `IF NOT EXISTS`이고 제약이 `CREATE TABLE` 안에 인라인되어 있어, **테이블이 이미 있는 기존 데이터베이스에서는 베이스라인 전체가 no-op**이다.

베이스라인은 손으로 고치지 않는다. 엔티티에서 재생성한다.

```bash
./gradlew schemaDump && python3 tools/generate_baseline.py
```

---

## 테스트 전략

운영과 **같은 DB 엔진** 위에서 Flyway 마이그레이션을 적용한 뒤 `ddl-auto=validate`로 검증한다. 엔티티와 마이그레이션이 어긋나면 테스트가 실패한다.

```
Docker 있음 → MariaDB 컨테이너 + Flyway + validate   (CI)
Docker 없음 → H2(MariaDB 모드) + create-drop 으로 강등 (로컬 편의)
```

테스트가 정상 경로보다 **실패 경로에 집중**되어 있다. 정상 경로는 원래도 동작했고, 돈이 새는 곳은 실패 경로였기 때문이다.

| 대상 | 검증 내용 |
|---|---|
| `TransferServiceIdempotencyTests` | 재요청·생체인증 실패·한도 초과·응답 불명 시 상태와 한도 |
| `TransferLimitGuardConcurrencyTests` | 40스레드 동시 요청에서 한도 초과 불가 |
| `TransferReconciliationTests` | 정산 확정·미출금 시에만 한도 복구 |
| `OutboxRelayTests` | 재시도·DEAD 격리·격리 후 릴레이 차단 없음 |
| `UserWithdrawalTests` | 무엇이 지워지고 무엇이 남는지 |
| `FieldEncryptorTests` | 키 불일치·변조 시 예외, 레거시 포맷 호환 |
| `EnumColumnWidthTests` | enum 상수가 컬럼 길이를 넘지 않는지 (DB 없이) |
| `SchemaMigrationDriftTests` | 마이그레이션 적용 여부·제약 존재 (MariaDB 전용) |

커버리지 게이트는 전체 33%, 돈 관련 클래스(`TransferService`, `TransferLimitGuard`, `FieldEncryptor`)는 70%로 CI에서 강제한다.

---

## 운영

배치는 전부 ShedLock으로 보호되어 인스턴스가 여러 대여도 한 번만 돈다. 스케줄 진입점과 실제 로직을 분리해 두어(`relay()` / `relayOnce()`) 테스트에서 락 인프라 없이 로직을 직접 검증할 수 있다.

| 배치 | 주기 | 역할 |
|---|---|---|
| 아웃박스 릴레이 | 1초 | 이벤트 발행·재시도·DEAD 격리 |
| 이체 정산 | 30초 | 미결 이체를 은행에 재조회해 확정 |
| 보호자 승인 만료 | 1분 | 기한 지난 승인 요청·주문 취소 |
| 마이데이터 토큰 갱신 | 15분 | 만료 임박 토큰 갱신 |
| 동의 만료 처리 | 1시간 | 수집 중단 + 데이터 파기, 7일 전 예고 |
| 거래 동기화 | 매일 04시 | 오픈뱅킹 거래내역 적재 |
| 감사 체인 검증 | 매일 04:30 | 해시 체인 무결성 |
| 보존기간 만료 파기 | 매일 03시 | 법정 보존기간 경과분 파기 |

알람은 [`infra/observability/alert-rules.yml`](infra/observability/alert-rules.yml)에 정의되어 있고, 각 알람에 대응하는 조치 API가 있다. 상세는 [운영 런북](docs/operations-runbook.md).

---

## 문서

- [운영 런북](docs/operations-runbook.md) — 배포·장애 대응·알람별 조치
- [마이데이터·오픈뱅킹 직접 등록 가이드](docs/마이데이터-오픈뱅킹-직접등록-가이드.md)

---

## 알려진 제약

솔직하게 남긴다.

- **부하 검증을 하지 않았다.** 커넥션 풀 크기(20)와 HTTP 클라이언트 설정은 합리적 기본값이지 실측 결과가 아니다.
- **키 로테이션 배치가 없다.** `FieldEncryptor`가 버전별 복호화와 `needsReEncryption()`을 지원하지만, 실제 재암호화를 수행하는 배치는 아직 없다. 현재는 로테이션할 키가 없어 우선순위를 낮췄다.
- **로그의 PII 노출을 감사하지 않았다.** 계좌번호·전화번호가 로그에 남는지 전수 확인이 필요하다.
- **백업 복구를 리허설하지 않았다.** `infra/scripts/backup-mariadb.sh`는 있으나 복구 절차를 실행해본 적이 없다.
