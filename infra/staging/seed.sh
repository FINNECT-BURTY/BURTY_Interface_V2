#!/usr/bin/env bash
# 스테이징에 부하 시험용 데이터를 넣는다.
#
# 빈 DB 를 상대로 한 측정은 인덱스도 옵티마이저도 실제와 다르게 동작해 아무것도
# 말해주지 않는다. infra/loadtest/seed.sql 과 같은 데이터를 쓴다.

set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${HERE}/../.." && pwd)"
COMPOSE="${ROOT}/docker-compose.staging.yml"
DB_PASSWORD="${DB_PASSWORD:-staging-password}"

echo "[seed] 스테이징 DB 에 표본 데이터 적재"
docker compose -f "${COMPOSE}" exec -T mariadb \
  mariadb -uroot -p"${DB_PASSWORD}" burty < "${ROOT}/infra/loadtest/seed.sql"

echo "[seed] 완료"
