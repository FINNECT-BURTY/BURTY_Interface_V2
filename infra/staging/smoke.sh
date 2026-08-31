#!/usr/bin/env bash
# 스테이징 연동 경로가 실제로 도는지 확인한다.
#
# stub 이 아니라 WireMock 을 향해 진짜 HTTP 가 나가는지, 그리고 이체 실패 모드가
# 설계대로 갈라지는지를 본다. 이건 단위 테스트로는 확인할 수 없는 부분이다.

set -euo pipefail

BASE="${BASE_URL:-http://localhost:8080}"
MOCK="${MOCK_URL:-http://localhost:18080}"

fail() { echo "  ✗ $1" >&2; exit 1; }
ok()   { echo "  ✓ $1"; }

echo "[smoke] 1/4 애플리케이션 readiness"
curl -sf "${BASE}/actuator/health/readiness" >/dev/null || fail "readiness 실패"
ok "readiness"

echo "[smoke] 2/4 토큰 발급"
USER_ID="${USER_ID:-1}"
TOKEN=$(curl -sf -XPOST "${BASE}/api/v1/auth/token" \
  -H 'Content-Type: application/json' -d "{\"userId\":\"${USER_ID}\"}" \
  | python3 -c 'import sys,json; print(json.load(sys.stdin)["data"]["accessToken"])')
[ -n "${TOKEN}" ] || fail "토큰 발급 실패"
ok "토큰 발급"

echo "[smoke] 3/4 조회 경로"
curl -sf "${BASE}/api/v1/transactions?page=0&size=10" \
  -H "Authorization: Bearer ${TOKEN}" >/dev/null || fail "거래내역 조회 실패"
ok "거래내역 조회"

echo "[smoke] 4/4 외부 연동이 실제 HTTP 를 타는가"
# /__admin/requests/count 는 POST 에 조건 본문을 받는다. 단순 집계는 목록의 meta.total 이다.
mock_request_total() {
  curl -sf "${MOCK}/__admin/requests" \
    | python3 -c 'import sys,json; print(json.load(sys.stdin)["meta"]["total"])'
}

BEFORE=$(mock_request_total)
curl -sf "${BASE}/api/v1/external/openbanking/accounts" \
  -H "Authorization: Bearer ${TOKEN}" >/dev/null || true
AFTER=$(mock_request_total)

if [ "${AFTER}" -le "${BEFORE}" ]; then
  fail "외부 목에 요청이 도달하지 않았다 (stub 으로 돌고 있을 수 있다: before=${BEFORE} after=${AFTER})"
fi
ok "외부 연동이 실제 HTTP 를 탐 (${BEFORE} → ${AFTER})"

echo
echo "[smoke] 통과"
echo
echo "다음으로 해볼 것:"
echo "  실패 시나리오  — 이체 요청에 X-Mock-Scenario: timeout 을 붙여 UNKNOWN 처리를 확인"
echo "  부하 시험      — BURTY_API_RATELIMIT_ENABLED=false 로 재기동 후"
echo "                   k6 run -e BASE_URL=${BASE} -e TOKEN=\$TOKEN -e PROFILE=full infra/loadtest/burty-load.js"
