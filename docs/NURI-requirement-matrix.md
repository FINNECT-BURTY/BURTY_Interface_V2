# NURI Requirement Matrix (v1.2 PDF)

## Feature Coverage

- `Easy-Read 월간 리포트`: **Partial**
  - Implemented: monthly batch/PDF generation in `application/service/MonthlyReportBatchService`.
  - Gap: strict policy enforcement for exactly 3 short sentences and stable action-card metadata in payload.
- `쉬운 말 AI 상담`: **Implemented**
  - Implemented: consult endpoint + easy-read transformation in `adapter/out/easyread/EasyReadEngineAdapter`.
- `3단계 권한 체계`: **Partial**
  - Implemented: `@AuthLevel` + interceptor.
  - Upgraded now: server-signed `X-Risk-Proof` token verification for level2/level3.
  - Gap: dedicated edge gateway still needed to fully match document architecture.
- `레벨3 생체인증`: **Implemented (core flow)**
  - Implemented: WebAuthn challenge/verification and transfer assertion checks.
  - Gap: expanded credential lifecycle policy (device binding, revocation UX, rotation policy) can be strengthened.
- `가족 공유/알림`: **Implemented**
  - Implemented: family alerts, consent, dashboard endpoints.
  - Upgraded now: realtime SSE stream endpoint and broker push.
- `이상거래 탐지`: **Implemented**
  - Implemented: night transfer/unregistered account/large transfer pattern checks in `NuriService`.
- `외부 API 연동`: **Partial**
  - Implemented: MyData/Kakao/Pension adapters with stub+real modes.
  - Gap: resilience policy (circuit breaker), richer error taxonomy, production runbook.
- `JWT + Redis 블랙리스트`: **Implemented**
  - Implemented: blacklist service with Redis + local fallback.
- `Audit Log`: **Implemented**
  - Implemented: audit persistence ports/adapters and service usage.
- `Security Gate Layer (Spring Cloud Gateway)`: **Not Implemented**
  - Current: MVC interceptor at application layer.
  - Needed: separate gateway/routing tier.

## Package Placement Review

- Requirement: code should live under `java/com/nuri`.
- Current status: satisfied.
  - All NURI modules are under `src/main/java/com/nuri/nuri/**`.
  - Hexagonal structure (`application/port`, `adapter`, `domain`) is also inside the same namespace tree.

## Priority Additions

1. Deploy dedicated gateway-based `Security Gate Layer`.
2. Add replay-protection storage for risk proofs (single-use nonce).
3. Add push channel integration (FCM/APNs) in addition to SSE.
4. Expand scenario E2E tests for level2/level3 proof handshake and alert stream behavior.
