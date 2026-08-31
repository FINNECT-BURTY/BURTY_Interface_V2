#!/usr/bin/env bash
# 이체 실패 시나리오를 스택 전체로 통과시켜 본다.
#
# 은행이 응답하지 않으면 출금됐는지 알 수 없다. 그때 실패로 확정하고 한도를 되돌리면
# 돈은 나갔는데 한도만 복구되고 사용자에게는 실패라고 알리게 된다.
#
# 단위 테스트는 예외 매핑까지만 확인한다. 컨트롤러 → 서비스 → 어댑터 → 은행 을
# 전부 통과시켜야 실제로 그렇게 도는지 알 수 있다.
#
# 전제조건: 사용자에게 오픈뱅킹 연동 기관이 등록돼 있어야 한다.
#   없으면 이체가 404(미연동)로 거절되고 이 스크립트는 아무것도 확인하지 못한다.
#   연동은 OAuth 링크 흐름을 거쳐야 만들어진다 (SQL 시딩으로는 access_token 암호화 때문에 안 된다).
#
# 사용:
#   ./infra/staging/transfer-scenario.sh
#   AMOUNT=99999 ./infra/staging/transfer-scenario.sh   # 한 건만
#
# 금액으로 목의 응답을 고른다 (infra/staging/wiremock/mappings 참고).
# 헤더는 앱을 거쳐 은행으로 전달되지 않기 때문에 요청 내용으로 분기한다.
#
#   99999  30초 지연  → 결과 불명
#   88888  503        → 결과 불명
#   77777  400        → 확정 실패
#   그 외   200        → 정상

set -euo pipefail

BASE="${BASE_URL:-http://localhost:8080}"
USER_ID="${USER_ID:-1}"
ORIGIN="${WEBAUTHN_ORIGIN:-http://localhost:8080}"
RP_ID="${WEBAUTHN_RP_ID:-localhost}"

fail() { echo "  ✗ $1" >&2; exit 1; }
ok()   { echo "  ✓ $1"; }

# 중첩 따옴표를 피하려고 키 경로를 인자로 받아 순회한다.
json() {
  python3 -c '
import sys, json
d = json.load(sys.stdin)
for k in sys.argv[1:]:
    d = d.get(k) if isinstance(d, dict) else None
print("" if d is None else d)
' "$@"
}

# WebAuthn 스텁이 받아들이는 페이로드. 서명은 검증하지 않고 기대값 문자열만 확인한다.
# 등록은 attestationObject 를, 인증은 signature 를 찾으므로 둘 다 넣는다.
# (운영에서 이 스텁이 켜져 있으면 기동이 막힌다 — ProdStartupValidator)
stub_payload() {
  python3 -c '
import json, sys
# 스텁은 "challenge":"..." 형태를 문자열로 찾는다. 콜론 뒤 공백이 있으면 못 찾는다.
inner = json.dumps(
    {
        "challenge": sys.argv[1],
        "origin": sys.argv[2],
        "rpId": sys.argv[3],
        "signature": "staging-stub-signature",
        "attestationObject": "staging-stub-attestation",
    },
    separators=(",", ":"),
)
print(json.dumps(inner))
' "$1" "$ORIGIN" "$RP_ID"
}

echo "[scenario] 토큰 발급"
TOKEN=$(curl -sf -XPOST "${BASE}/api/v1/auth/token" \
  -H 'Content-Type: application/json' -d "{\"userId\":\"${USER_ID}\"}" \
  | json data accessToken)
[ -n "${TOKEN}" ] || fail "토큰 발급 실패"

# 인증은 신뢰 기기가 있어야 한다. 등록 의식을 먼저 거쳐 deviceToken 을 받는다.
echo "[scenario] 기기 등록 (WebAuthn 스텁)"
REG_CHALLENGE=$(curl -s -XPOST "${BASE}/api/v1/security/webauthn/register/begin" \
  -H "Authorization: Bearer ${TOKEN}" -H 'Content-Type: application/json' -d '{}' \
  | json data challengeId)
[ -n "${REG_CHALLENGE}" ] || fail "등록 챌린지 발급 실패"

REG_BODY=$(printf '{"challengeId":"%s","payload":%s,"deviceFingerprint":"staging-device","platform":"WEB","biometricType":"FINGERPRINT"}' \
  "${REG_CHALLENGE}" "$(stub_payload "${REG_CHALLENGE}")")
REG_RESP=$(curl -s -XPOST "${BASE}/api/v1/security/webauthn/register/finish" \
  -H "Authorization: Bearer ${TOKEN}" -H 'Content-Type: application/json' -d "${REG_BODY}")
DEVICE_TOKEN=$(printf '%s' "${REG_RESP}" | json data deviceToken)
[ -n "${DEVICE_TOKEN}" ] || fail "기기 등록 실패: ${REG_RESP}"
ok "기기 등록"

echo "[scenario] 단계 인증 (WebAuthn 스텁)"
BEGIN_RESP=$(curl -s -XPOST "${BASE}/api/v1/security/webauthn/authenticate/begin" \
  -H "Authorization: Bearer ${TOKEN}" -H 'Content-Type: application/json' -d '{}')
CHALLENGE_ID=$(printf '%s' "${BEGIN_RESP}" | json data challengeId)
[ -n "${CHALLENGE_ID}" ] || fail "챌린지 발급 실패: ${BEGIN_RESP}"

AUTH_BODY=$(printf '{"challengeId":"%s","payload":%s,"deviceToken":"%s"}' \
  "${CHALLENGE_ID}" "$(stub_payload "${CHALLENGE_ID}")" "${DEVICE_TOKEN}")
FINISH_RESP=$(curl -s -XPOST "${BASE}/api/v1/security/webauthn/authenticate/finish" \
  -H "Authorization: Bearer ${TOKEN}" -H 'Content-Type: application/json' -d "${AUTH_BODY}")
RISK_PROOF=$(printf '%s' "${FINISH_RESP}" | json data riskProof)
[ -n "${RISK_PROOF}" ] || fail "LEVEL_3 증명 발급 실패: ${FINISH_RESP}"
ok "단계 인증 통과"

transfer() {
  local amount="$1" label="$2"
  local key status body
  key="scenario-$(date +%s%N)"
  body=$(printf '{"fromAccount":"1234567890","toAccount":"9876543210","amount":%s,"description":"staging","assertionToken":"staging","idempotencyKey":"%s"}' \
    "${amount}" "${key}")
  status=$(curl -s -o /tmp/burty-transfer.json -w '%{http_code}' \
    -XPOST "${BASE}/api/v1/transfers" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H "X-Risk-Proof: ${RISK_PROOF}" \
    -H 'Content-Type: application/json' -d "${body}" || true)
  echo "  ${label} (amount=${amount}) → HTTP ${status}: $(head -c 200 /tmp/burty-transfer.json)" >&2
  printf '%s' "${status}"
}

if [ -n "${AMOUNT:-}" ]; then
  transfer "${AMOUNT}" "지정 금액" > /dev/null
  exit 0
fi

echo "[scenario] 은행 타임아웃 — 결과를 알 수 없다"
STATUS=$(transfer 99999 "타임아웃")
CODE=$(json errorCode < /tmp/burty-transfer.json)
if [ "${STATUS}" -ge 200 ] && [ "${STATUS}" -lt 300 ]; then
  fail "은행 응답을 받지 못했는데 성공으로 응답했다"
fi
# 거절 이유가 무엇이든 2xx 만 아니면 통과시키면, 미연동(404) 같은 무관한 실패도 통과한다.
# 결과 불명(9000)인지까지 확인해야 실제로 그 경로를 지났다고 말할 수 있다.
[ "${CODE}" = "9000" ] || fail "결과 불명(9000)이 아니라 ${CODE} 로 거절됐다 — 이 경로를 지나지 않았다"
ok "결과 불명으로 처리 (errorCode=9000)"

echo "[scenario] 은행 5xx — 처리 중이었을 수 있다"
STATUS=$(transfer 88888 "서버 오류")
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
