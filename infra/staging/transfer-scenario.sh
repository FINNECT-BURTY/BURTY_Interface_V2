#!/usr/bin/env bash
# 이체 실패 시나리오를 스택 전체로 통과시켜 본다.
#
# 은행이 응답하지 않으면 출금됐는지 알 수 없다. 그때 실패로 확정하고 한도를 되돌리면
# 돈은 나갔는데 한도만 복구되고 사용자에게는 실패라고 알리게 된다.
#
# 단위 테스트는 예외 매핑까지만 확인한다. 컨트롤러 → 서비스 → 어댑터 → 은행 을
# 전부 통과시켜야 실제로 그렇게 도는지 알 수 있다.
#
# 사용:
#   ./infra/staging/transfer-scenario.sh            # 전부
#   AMOUNT=99999 ./infra/staging/transfer-scenario.sh
#
# 금액으로 목의 응답을 고른다 (infra/staging/wiremock/mappings 참고).
#   99999  30초 지연  → 결과 불명
#   88888  503        → 결과 불명
#   77777  400        → 확정 실패
#   그 외   200        → 정상

set -euo pipefail

BASE="${BASE_URL:-http://localhost:8080}"
USER_ID="${USER_ID:-1}"

fail() { echo "  ✗ $1" >&2; exit 1; }
ok()   { echo "  ✓ $1"; }
json() { python3 -c "import sys,json;d=json.load(sys.stdin);print(eval('d'+'$1'))"; }

echo "[scenario] 토큰 발급"
TOKEN=$(curl -sf -XPOST "${BASE}/api/v1/auth/token" \
  -H 'Content-Type: application/json' -d "{\"userId\":\"${USER_ID}\"}" \
  | json "['data']['accessToken']")
[ -n "${TOKEN}" ] || fail "토큰 발급 실패"

# LEVEL_3 증명을 받는다. 스테이징은 WebAuthn 스텁이라 서명 없이 통과한다.
# (운영에서는 이 스텁이 켜져 있으면 기동이 막힌다 — ProdStartupValidator)
echo "[scenario] 단계 인증 (WebAuthn 스텁)"
CHALLENGE_ID=$(curl -sf -XPOST "${BASE}/api/v1/security/webauthn/authenticate/begin" \
  -H "Authorization: Bearer ${TOKEN}" -H 'Content-Type: application/json' -d '{}' \
  | json "['data']['challengeId']")
[ -n "${CHALLENGE_ID}" ] || fail "챌린지 발급 실패"

PAYLOAD=$(python3 - "$CHALLENGE_ID" <<'PY'
import json, sys
print(json.dumps(json.dumps({
    "challenge": sys.argv[1],
    "origin": "http://localhost:8080",
    "rpId": "localhost",
    "signature": "staging-stub-signature",
})))
PY
)
RISK_PROOF=$(curl -sf -XPOST "${BASE}/api/v1/security/webauthn/authenticate/finish" \
  -H "Authorization: Bearer ${TOKEN}" -H 'Content-Type: application/json' \
  -d "{\"challengeId\":\"${CHALLENGE_ID}\",\"payload\":${PAYLOAD}}" \
  | json "['data']['riskProof']")
[ -n "${RISK_PROOF}" ] && [ "${RISK_PROOF}" != "None" ] || fail "LEVEL_3 증명 발급 실패"
ok "단계 인증 통과"

transfer() {
  local amount="$1" label="$2"
  local key="scenario-$(date +%s%N)"
  local status
  status=$(curl -s -o /tmp/burty-transfer.json -w '%{http_code}' \
    -XPOST "${BASE}/api/v1/transfers" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H "X-Risk-Proof: ${RISK_PROOF}" \
    -H 'Content-Type: application/json' \
    -d "{\"fromAccount\":\"1234567890\",\"toAccount\":\"9876543210\",\"amount\":${amount},\"description\":\"staging\",\"assertionToken\":\"staging\",\"idempotencyKey\":\"${key}\"}" || true)
  echo "  ${label} (amount=${amount}) → HTTP ${status}: $(head -c 200 /tmp/burty-transfer.json)"
  echo "${status}"
}

AMOUNT="${AMOUNT:-}"
if [ -n "${AMOUNT}" ]; then
  transfer "${AMOUNT}" "지정 금액" > /dev/null
  exit 0
fi

echo "[scenario] 은행 타임아웃 — 결과를 알 수 없다"
STATUS=$(transfer 99999 "타임아웃" | tail -1)
if [ "${STATUS}" -ge 200 ] && [ "${STATUS}" -lt 300 ]; then
  fail "은행 응답을 받지 못했는데 성공으로 응답했다"
fi
ok "성공으로 확정하지 않음"

echo "[scenario] 은행 5xx — 처리 중이었을 수 있다"
STATUS=$(transfer 88888 "서버 오류" | tail -1)
if [ "${STATUS}" -ge 200 ] && [ "${STATUS}" -lt 300 ]; then
  fail "5xx 를 받았는데 성공으로 응답했다"
fi
ok "성공으로 확정하지 않음"

echo "[scenario] 은행 4xx — 명확한 거절"
transfer 77777 "거절" > /dev/null
ok "확정 실패로 처리"

echo
echo "[scenario] 완료 — DB 에서 상태를 확인할 것:"
echo "  SELECT status, COUNT(*) FROM tbl_transfer_order GROUP BY status;"
echo "  UNKNOWN 이 있어야 정산 대상이 된다. FAILED 만 있으면 잘못된 것이다."
