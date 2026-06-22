#!/usr/bin/env bash
# MariaDB 논리 백업 — cron 또는 수동 실행
#
# 사용:
#   DB_HOST=localhost DB_PORT=3306 DB_NAME=burty DB_USER=root DB_PASSWORD=secret \
#     ./infra/scripts/backup-mariadb.sh
#
# 환경변수:
#   BACKUP_DIR  (default: ./backups)
#   RETENTION_DAYS (default: 14)

set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-./backups}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"
TIMESTAMP="$(date +%Y%m%d_%H%M%S)"
FILENAME="burty_${DB_NAME:-burty}_${TIMESTAMP}.sql.gz"

mkdir -p "${BACKUP_DIR}"

echo "[backup] dumping ${DB_NAME:-burty} from ${DB_HOST:-localhost}:${DB_PORT:-3306}"

mysqldump \
  -h "${DB_HOST:-localhost}" \
  -P "${DB_PORT:-3306}" \
  -u "${DB_USER:-root}" \
  -p"${DB_PASSWORD:?DB_PASSWORD is required}" \
  --single-transaction \
  --routines \
  --triggers \
  "${DB_NAME:-burty}" | gzip > "${BACKUP_DIR}/${FILENAME}"

echo "[backup] saved ${BACKUP_DIR}/${FILENAME}"

find "${BACKUP_DIR}" -name 'burty_*.sql.gz' -mtime +"${RETENTION_DAYS}" -delete
echo "[backup] pruned files older than ${RETENTION_DAYS} days"
