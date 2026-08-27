-- 이체 정합성 + 트랜잭셔널 아웃박스
--
-- 1) 이체 주문에 (user_id, idempotency_key) 복합 유니크를 건다.
--    멱등키 중복 판정을 애플리케이션 조회가 아니라 DB 제약이 하게 만들기 위한 것이다.
--    기존의 전역 유니크(uk_idempotency)는 사용자가 서로 다른데 같은 키를 쓰면 재요청 응답 대신
--    500 을 내므로 제거한다.
-- 2) 결과 불명(UNKNOWN) 건의 정산 추적 컬럼을 추가한다.
-- 3) 아웃박스 테이블을 실제 사용 가능한 형태로 만든다.

-- ---------------------------------------------------------------------------
-- 1) 이체 주문 유니크 제약 교체
-- ---------------------------------------------------------------------------
SET @legacy := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'tbl_transfer_order'
      AND INDEX_NAME = 'uk_idempotency');
SET @sql := IF(@legacy > 0,
               'ALTER TABLE tbl_transfer_order DROP INDEX uk_idempotency',
               'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 2) 정산 추적 컬럼
--    기존 행에는 requested_at 이 없으므로, 알 수 있는 가장 가까운 값으로 채운다.
-- ---------------------------------------------------------------------------
ALTER TABLE tbl_transfer_order
    ADD COLUMN IF NOT EXISTS requested_at DATETIME(6) NULL AFTER failed_reason,
    ADD COLUMN IF NOT EXISTS next_reconcile_at DATETIME(6) NULL AFTER requested_at,
    ADD COLUMN IF NOT EXISTS reconcile_attempts INT NOT NULL DEFAULT 0 AFTER next_reconcile_at;

UPDATE tbl_transfer_order
SET requested_at = COALESCE(requested_at, executed_at, scheduled_at, NOW(6))
WHERE requested_at IS NULL;

ALTER TABLE tbl_transfer_order
    MODIFY COLUMN requested_at DATETIME(6) NOT NULL;

-- 결과 불명/실행중 상태 추가에 대응 (enum 이 아니라 varchar 이면 no-op).
ALTER TABLE tbl_transfer_order
    MODIFY COLUMN status VARCHAR(20) NOT NULL;

-- (user_id, idempotency_key) 유니크가 이미 있으면 (신규 환경: V3 베이스라인이 생성) 다시 만들지 않는다.
SET @has_uk := (
    SELECT COUNT(*) FROM (
        SELECT INDEX_NAME
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'tbl_transfer_order'
          AND NON_UNIQUE = 0
          AND COLUMN_NAME IN ('user_id', 'idempotency_key')
        GROUP BY INDEX_NAME
        HAVING COUNT(DISTINCT COLUMN_NAME) = 2) x);
SET @sql := IF(@has_uk = 0,
               'CREATE UNIQUE INDEX uk_transfer_order_idempotency ON tbl_transfer_order (user_id, idempotency_key)',
               'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
CREATE INDEX IF NOT EXISTS idx_transfer_order_user
    ON tbl_transfer_order (user_id, order_id);
CREATE INDEX IF NOT EXISTS idx_transfer_order_reconcile
    ON tbl_transfer_order (status, next_reconcile_at);

-- ---------------------------------------------------------------------------
-- 3) 아웃박스
--    레거시 tbl_outbox_event 는 코드에서 한 번도 참조된 적이 없다 (항상 비어 있음).
--    구 스키마는 aggregate_id 가 BINARY(16) 이라 현재 BIGINT PK 모델과 맞지 않고 재시도 컬럼도 없다.
--    데이터 손실 위험을 피하기 위해 삭제가 아니라 이름을 바꿔 보존한다.
-- ---------------------------------------------------------------------------
SET @has_legacy_outbox := (
    SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'tbl_outbox_event'
      AND NOT EXISTS (
          SELECT 1 FROM information_schema.COLUMNS
          WHERE TABLE_SCHEMA = DATABASE()
            AND TABLE_NAME = 'tbl_outbox_event'
            AND COLUMN_NAME = 'next_attempt_at'));
SET @sql := IF(@has_legacy_outbox > 0,
               'RENAME TABLE tbl_outbox_event TO tbl_outbox_event_legacy_unused',
               'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS tbl_outbox_event (
    event_id BIGINT NOT NULL AUTO_INCREMENT,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload LONGTEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    published_at DATETIME(6) NULL,
    last_error VARCHAR(500) NULL,
    PRIMARY KEY (event_id)
) ENGINE=InnoDB COMMENT='트랜잭셔널 아웃박스';

CREATE INDEX IF NOT EXISTS idx_outbox_dispatch
    ON tbl_outbox_event (status, next_attempt_at);
CREATE INDEX IF NOT EXISTS idx_outbox_aggregate
    ON tbl_outbox_event (aggregate_type, aggregate_id);
