#!/usr/bin/env bash
# 백업·복구 리허설 — 백업을 뜨고 즉시 별도 DB 로 복구해 검증한다.
#
# 사용:
#   DB_HOST=localhost DB_NAME=burty DB_USER=root DB_PASSWORD=secret \
#     ./infra/scripts/backup-restore-rehearsal.sh
#
# 복구되지 않는 백업은 백업이 아니다. 이 스크립트를 주기적으로 돌려
# "백업이 실제로 복구 가능한 상태인지" 를 계속 확인한다.
#
# 운영 DB 는 건드리지 않는다. 읽기(덤프)만 하고 별도 DB 에 복구한다.
# 주의: 복구 대상 DB 는 매번 DROP 후 재생성된다.

set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REHEARSAL_DB="${REHEARSAL_DB:-burty_rehearsal}"
BACKUP_DIR="${BACKUP_DIR:-./backups/rehearsal}"
KEEP_RESTORED="${KEEP_RESTORED:-no}"

: "${DB_PASSWORD:?DB_PASSWORD is required}"

if [ "${REHEARSAL_DB}" = "${DB_NAME:-burty}" ]; then
  echo "거부: 리허설 대상이 운영 DB 와 같습니다." >&2
  exit 1
fi

START=$(date +%s)
echo "=== 백업·복구 리허설 시작 $(date -u +%Y-%m-%dT%H:%M:%SZ) ==="

echo
echo "--- 1/3 백업 ---"
BACKUP_DIR="${BACKUP_DIR}" "${HERE}/backup-mariadb.sh"

LATEST=$(find "${BACKUP_DIR}" -name 'burty_*.sql.gz' -print0 | xargs -0 ls -t 2>/dev/null | head -1)
: "${LATEST:?백업 파일을 찾지 못했습니다}"
BACKUP_DONE=$(date +%s)

echo
echo "--- 2/3 복구 ---"
"${HERE}/restore-mariadb.sh" "${LATEST}" "${REHEARSAL_DB}"
RESTORE_DONE=$(date +%s)

echo
echo "--- 3/3 정리 ---"
if [ "${KEEP_RESTORED}" = "yes" ]; then
  echo "복구본 유지: ${REHEARSAL_DB} (KEEP_RESTORED=yes)"
else
  export MYSQL_PWD="${DB_PASSWORD}"
  CLIENT_BIN="$(command -v mariadb || command -v mysql)"
  "${CLIENT_BIN}" -h "${DB_HOST:-localhost}" -P "${DB_PORT:-3306}" -u "${DB_USER:-root}" \
    -e "DROP DATABASE IF EXISTS \`${REHEARSAL_DB}\`;"
  echo "복구본 정리 완료"
fi

echo
echo "=== 리허설 성공 ==="
echo "  백업 소요 : $((BACKUP_DONE - START))초"
echo "  복구 소요 : $((RESTORE_DONE - BACKUP_DONE))초  ← RTO 추정치"
echo "  전체      : $((RESTORE_DONE - START))초"
echo
echo "RPO 는 백업 주기로 결정된다. cron 주기를 확인할 것."
