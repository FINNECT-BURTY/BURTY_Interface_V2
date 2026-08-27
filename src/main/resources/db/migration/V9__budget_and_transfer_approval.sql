-- 예산 + 보호자 사전 승인
--
-- 1) 예산: 거래 분류와 현금흐름 예측은 있었지만 정작 "이번 달에 얼마 쓸 수 있는지" 를
--    표현할 데이터가 없었다. 카테고리별 예산과 전체 예산을 동시에 둘 수 있게 한다.
-- 2) 보호자 사전 승인: 기존 가족 보호는 이상 이체를 사후 통지만 했다. 알림 시점에는 이미
--    출금이 끝난 뒤라 보이스피싱 피해를 막지 못한다. 고액 이체를 보류하고 승인을 받는다.

CREATE TABLE IF NOT EXISTS tbl_budget (
    budget_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL,
    category_code VARCHAR(40) NULL COMMENT 'NULL 이면 전체 지출 예산',
    period_type VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    amount BIGINT NOT NULL,
    alert_threshold_percent INT NOT NULL DEFAULT 80,
    active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (budget_id),
    CONSTRAINT uk_budget_user_category_period UNIQUE (user_id, category_code, period_type)
) ENGINE=InnoDB COMMENT='예산';

CREATE INDEX IF NOT EXISTS idx_budget_user ON tbl_budget (user_id, active);

-- 예산 경고 발송 이력.
-- 유니크 제약이 핵심이다. 예산 초과 상태는 그 달 내내 유지되므로, 없으면 거래마다 같은 경고가 반복된다.
CREATE TABLE IF NOT EXISTS tbl_budget_alert (
    alert_id BIGINT NOT NULL AUTO_INCREMENT,
    budget_id BIGINT NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    period_key VARCHAR(20) NOT NULL COMMENT '예: 2026-08',
    level VARCHAR(20) NOT NULL,
    spent_amount BIGINT NOT NULL,
    budget_amount BIGINT NOT NULL,
    notified_at DATETIME(6) NOT NULL,
    PRIMARY KEY (alert_id),
    CONSTRAINT uk_budget_alert_period_level UNIQUE (budget_id, period_key, level)
) ENGINE=InnoDB COMMENT='예산 경고 발송 이력';

CREATE TABLE IF NOT EXISTS tbl_transfer_approval (
    approval_id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    requester_user_id VARCHAR(64) NOT NULL,
    guardian_user_id VARCHAR(64) NOT NULL,
    amount BIGINT NOT NULL,
    to_account_masked VARCHAR(80) NOT NULL,
    reason VARCHAR(200) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    requested_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    decided_at DATETIME(6) NULL,
    decision_note VARCHAR(200) NULL,
    PRIMARY KEY (approval_id)
) ENGINE=InnoDB COMMENT='이체 보호자 사전 승인';

CREATE INDEX IF NOT EXISTS idx_approval_guardian
    ON tbl_transfer_approval (guardian_user_id, status);
CREATE INDEX IF NOT EXISTS idx_approval_order ON tbl_transfer_approval (order_id);
CREATE INDEX IF NOT EXISTS idx_approval_expiry ON tbl_transfer_approval (status, expires_at);

-- 보호자 권한에 VIEW_ALERT_AND_APPROVE 추가 (enum → varchar 이면 no-op).
ALTER TABLE tbl_guardian_link MODIFY COLUMN permission VARCHAR(30) NOT NULL;
