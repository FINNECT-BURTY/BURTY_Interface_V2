#!/usr/bin/env bash
# MariaDB 논리 백업 — cron 또는 수동 실행
#
# 사용:
#   DB_HOST=localhost DB_NAME=burty DB_USER=root DB_PASSWORD=secret \
#     ./infra/scripts/backup-mariadb.sh
#
# 환경변수:
#   BACKUP_DIR      (기본: ./backups)
#   RETENTION_DAYS  (기본: 14)
#   MIN_BACKUP_KB   (기본: 16)  이보다 작으면 실패로 본다
#
# 설계 메모:
#   - 비밀번호를 명령행에 두지 않는다. ps 로 다른 사용자에게 보인다.
#     MYSQL_PWD 환경변수로 넘긴다.
#   - 덤프 직후 무결성을 검증한다. 검증하지 않은 백업은 백업이 아니다.
#     gzip 이 잘려도 복구를 시도하기 전까지는 아무도 모른다.
#   - 바이너리 로그 위치를 함께 남긴다. 없으면 시점 복구(PITR)가 불가능하다.
#   - 오래된 파일을 지우기 전에 최신 백업이 성공했는지 먼저 확인한다.
#     백업이 며칠 멈춰 있었다면 정리만 돌아 전부 사라진다.

set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-./backups}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"
MIN_BACKUP_KB="${MIN_BACKUP_KB:-16}"
DB_NAME="${DB_NAME:-burty}"
TIMESTAMP="$(date +%Y%m%d_%H%M%S)"
BASENAME="burty_${DB_NAME}_${TIMESTAMP}"
# sha256 도구 이름이 플랫폼마다 다르다 (Linux: sha256sum, macOS: shasum).
sha256_of() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | awk '{print $1}'
  else
    shasum -a 256 "$1" | awk '{print $1}'
  fi
}

DUMP_PATH="${BACKUP_DIR}/${BASENAME}.sql.gz"
META_PATH="${BACKUP_DIR}/${BASENAME}.meta"

# MariaDB 11 부터 mariadb-dump 가 정식 이름이고 mysqldump 는 별칭이다.
DUMP_BIN="$(command -v mariadb-dump || command -v mysqldump)"
: "${DUMP_BIN:?mariadb-dump 또는 mysqldump 가 필요합니다}"

: "${DB_PASSWORD:?DB_PASSWORD is required}"
export MYSQL_PWD="${DB_PASSWORD}"

mkdir -p "${BACKUP_DIR}"
echo "[backup] ${DB_NAME} @ ${DB_HOST:-localhost}:${DB_PORT:-3306} → ${DUMP_PATH}"

# 바이너리 로그 위치를 덤프에 주석으로 남기면 복구 후 PITR 기준점이 된다.
# 다만 이 옵션은 환경을 많이 탄다.
#   - 이름이 다르다: MySQL 8 / MariaDB 11.4+ 는 --source-data, 그 이전은 --master-data
#   - 서버에 바이너리 로그가 꺼져 있으면 옵션 자체가 에러다
# 백업은 PITR 기준점이 없더라도 떠야 한다. 그래서 지원 여부를 확인해 있을 때만 붙인다.
BINLOG_OPT=""
if [ "${BINLOG_POS:-auto}" != "no" ]; then
  CLIENT_BIN="$(command -v mariadb || command -v mysql || true)"
  LOG_BIN="OFF"
  if [ -n "${CLIENT_BIN}" ]; then
    LOG_BIN=$("${CLIENT_BIN}" -h "${DB_HOST:-localhost}" -P "${DB_PORT:-3306}" \
      -u "${DB_USER:-root}" -N -B -e "SELECT @@log_bin;" 2>/dev/null || echo 0)
  fi
  if [ "${LOG_BIN}" = "1" ] || [ "${LOG_BIN}" = "ON" ]; then
    if "${DUMP_BIN}" --help 2>/dev/null | grep -q -- '--source-data'; then
      BINLOG_OPT="--source-data=2"
    elif "${DUMP_BIN}" --help 2>/dev/null | grep -q -- '--master-data'; then
      BINLOG_OPT="--master-data=2"
    fi
  fi
fi
if [ -z "${BINLOG_OPT}" ]; then
  echo "[backup] 주의 — 바이너리 로그 위치를 기록하지 않는다. 이 백업으로는 PITR 이 불가능하다." >&2
fi

# --single-transaction: InnoDB 를 잠그지 않고 일관된 스냅샷을 뜬다.
"${DUMP_BIN}" \
  -h "${DB_HOST:-localhost}" \
  -P "${DB_PORT:-3306}" \
  -u "${DB_USER:-root}" \
  --single-transaction \
  ${BINLOG_OPT} \
  --routines \
  --triggers \
  --events \
  "${DB_NAME}" | gzip > "${DUMP_PATH}"

# ── 검증 ────────────────────────────────────────────────────────────────────
# 1) gzip 스트림이 온전한가
if ! gzip -t "${DUMP_PATH}"; then
  echo "[backup] FAILED — gzip 무결성 검사 실패. 파일을 제거한다." >&2
  rm -f "${DUMP_PATH}"
  exit 1
fi

# 2) 크기가 최소 기준 이상인가 (빈 덤프 방지)
SIZE_KB=$(( $(wc -c < "${DUMP_PATH}") / 1024 ))
if [ "${SIZE_KB}" -lt "${MIN_BACKUP_KB}" ]; then
  echo "[backup] FAILED — 덤프가 너무 작다 (${SIZE_KB}KB < ${MIN_BACKUP_KB}KB)" >&2
  rm -f "${DUMP_PATH}"
  exit 1
fi

# 3) 핵심 테이블이 실제로 들어 있는가
for t in tbl_user tbl_transfer_order flyway_schema_history; do
  if ! gzip -dc "${DUMP_PATH}" | grep -q "CREATE TABLE \`${t}\`"; then
    echo "[backup] FAILED — ${t} 이(가) 덤프에 없다" >&2
    rm -f "${DUMP_PATH}"
    exit 1
  fi
done

# ── 메타데이터 ──────────────────────────────────────────────────────────────
{
  echo "created_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "database=${DB_NAME}"
  echo "source=${DB_HOST:-localhost}:${DB_PORT:-3306}"
  echo "size_kb=${SIZE_KB}"
  echo "sha256=$(sha256_of "${DUMP_PATH}")"
  # 덤프에 기록된 바이너리 로그 위치 (PITR 기준점)
  gzip -dc "${DUMP_PATH}" | grep -m1 'CHANGE MASTER TO\|CHANGE REPLICATION SOURCE TO' || echo "binlog_position=unavailable"
} > "${META_PATH}"

echo "[backup] OK ${SIZE_KB}KB — 메타: ${META_PATH}"

# ── 정리 ────────────────────────────────────────────────────────────────────
# 방금 만든 백업이 검증을 통과했을 때만 오래된 파일을 지운다.
# 백업이 며칠 멈춰 있었다면 정리만 돌아 전부 사라지는 사고를 막는다.
DELETED=$(find "${BACKUP_DIR}" -name 'burty_*.sql.gz' -mtime +"${RETENTION_DAYS}" -print -delete | wc -l | tr -d ' ')
find "${BACKUP_DIR}" -name 'burty_*.meta' -mtime +"${RETENTION_DAYS}" -delete
echo "[backup] ${RETENTION_DAYS}일 초과 ${DELETED}건 정리"
