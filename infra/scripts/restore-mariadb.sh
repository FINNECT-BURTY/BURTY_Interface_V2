#!/usr/bin/env bash
# MariaDB 논리 백업 복구
#
# 사용:
#   DB_HOST=localhost DB_USER=root DB_PASSWORD=secret \
#     ./infra/scripts/restore-mariadb.sh ./backups/burty_burty_20260828_030000.sql.gz [대상DB명]
#
# 대상 DB 명을 생략하면 burty_restore 로 복구한다.
# 운영 DB 를 덮어쓰려면 명시적으로 이름을 주고 CONFIRM=yes 를 설정해야 한다.

set -euo pipefail

DUMP_FILE="${1:?복구할 덤프 파일 경로가 필요합니다}"
TARGET_DB="${2:-burty_restore}"

: "${DB_PASSWORD:?DB_PASSWORD is required}"
export MYSQL_PWD="${DB_PASSWORD}"

CLIENT_BIN="$(command -v mariadb || command -v mysql)"
: "${CLIENT_BIN:?mariadb 또는 mysql 클라이언트가 필요합니다}"

run_sql() {
  "${CLIENT_BIN}" -h "${DB_HOST:-localhost}" -P "${DB_PORT:-3306}" \
    -u "${DB_USER:-root}" --batch --skip-column-names -e "$1"
}

# 운영 DB 를 덮어쓰는 것은 사고로 일어나기 쉽다. 명시적 확인을 요구한다.
# sha256 도구 이름이 플랫폼마다 다르다 (Linux: sha256sum, macOS: shasum).
sha256_of() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | awk '{print $1}'
  else
    shasum -a 256 "$1" | awk '{print $1}'
  fi
}

if [ "${TARGET_DB}" = "${DB_NAME:-burty}" ] && [ "${CONFIRM:-no}" != "yes" ]; then
  echo "거부: 대상이 운영 DB(${TARGET_DB}) 입니다. 의도한 것이라면 CONFIRM=yes 를 설정하세요." >&2
  exit 1
fi

echo "[restore] 무결성 확인: ${DUMP_FILE}"
gzip -t "${DUMP_FILE}" || { echo "[restore] FAILED — 손상된 덤프" >&2; exit 1; }

# 메타 파일이 있으면 체크섬을 대조한다. 전송 중 손상을 잡는다.
META_FILE="${DUMP_FILE%.sql.gz}.meta"
if [ -f "${META_FILE}" ]; then
  EXPECTED=$(grep '^sha256=' "${META_FILE}" | cut -d= -f2)
  ACTUAL=$(sha256_of "${DUMP_FILE}")
  if [ -n "${EXPECTED}" ] && [ "${EXPECTED}" != "${ACTUAL}" ]; then
    echo "[restore] FAILED — 체크섬 불일치" >&2
    exit 1
  fi
  echo "[restore] 체크섬 일치"
fi

echo "[restore] ${TARGET_DB} 생성"
run_sql "DROP DATABASE IF EXISTS \`${TARGET_DB}\`;
         CREATE DATABASE \`${TARGET_DB}\`
           DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;"

echo "[restore] 적재 중 (크기에 따라 수 분 소요)"
gzip -dc "${DUMP_FILE}" | "${CLIENT_BIN}" \
  -h "${DB_HOST:-localhost}" -P "${DB_PORT:-3306}" -u "${DB_USER:-root}" "${TARGET_DB}"

# ── 복구 검증 ───────────────────────────────────────────────────────────────
# 복구가 "끝났다" 와 "쓸 수 있다" 는 다르다. 실제로 확인한다.
echo "[restore] 검증"

TABLES=$(run_sql "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${TARGET_DB}';")
echo "  테이블 ${TABLES}개"
[ "${TABLES}" -ge 40 ] || { echo "[restore] FAILED — 테이블 수가 비정상 (${TABLES})" >&2; exit 1; }

# Flyway 이력이 살아 있어야 애플리케이션이 validate 를 통과한다.
FLYWAY=$(run_sql "SELECT COUNT(*) FROM \`${TARGET_DB}\`.flyway_schema_history WHERE success=1;" 2>/dev/null || echo 0)
echo "  Flyway 적용 이력 ${FLYWAY}건"
[ "${FLYWAY}" -gt 0 ] || { echo "[restore] FAILED — Flyway 이력이 없다" >&2; exit 1; }

# 핵심 테이블이 조회 가능한지 (구조 손상 탐지)
for t in tbl_user tbl_transfer_order tbl_audit_log; do
  run_sql "SELECT COUNT(*) FROM \`${TARGET_DB}\`.${t};" >/dev/null \
    || { echo "[restore] FAILED — ${t} 조회 불가" >&2; exit 1; }
done

# 감사 로그 해시 체인이 복구 후에도 이어지는지 표본 확인
CHAIN_GAPS=$(run_sql "
  SELECT COUNT(*) FROM (
    SELECT chain_seq, LAG(chain_seq) OVER (ORDER BY chain_seq) AS prev
    FROM \`${TARGET_DB}\`.tbl_audit_log WHERE chain_seq IS NOT NULL
  ) x WHERE prev IS NOT NULL AND chain_seq <> prev + 1;" 2>/dev/null || echo 0)
echo "  감사 체인 순번 불연속 ${CHAIN_GAPS}건"

echo "[restore] OK — ${TARGET_DB} 복구 완료"
echo
echo "다음 단계:"
echo "  1. 애플리케이션을 ${TARGET_DB} 로 띄워 ddl-auto=validate 통과를 확인한다"
echo "  2. 감사 체인 검증 배치를 수동 실행한다"
echo "  3. 확인 후 ${TARGET_DB} 를 정리한다"
