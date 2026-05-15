-- ----------------------------------------------------------------------------
-- DATABASE
-- ----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS burty
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE burty;

-- ----------------------------------------------------------------------------
-- 01. USER (암호화 포함)
-- ----------------------------------------------------------------------------
CREATE TABLE tbl_user (
    user_id BINARY(16) NOT NULL COMMENT 'UUID',

    ci_hash CHAR(64) NOT NULL COMMENT 'CI SHA-256 (검색용)',
    phone_hash CHAR(64) NOT NULL COMMENT '전화번호 SHA-256',

    ci_encrypted VARBINARY(512) NOT NULL COMMENT 'CI 암호화 (AES-GCM)',
    phone_encrypted VARBINARY(256) NOT NULL COMMENT '전화번호 암호화',

    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    failed_login_count INT DEFAULT 0 COMMENT '로그인 실패 횟수',
    last_login_at DATETIME(6),

    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (user_id),
    UNIQUE KEY uk_ci (ci_hash),
    UNIQUE KEY uk_phone (phone_hash)
) ENGINE=InnoDB COMMENT='사용자';

-- ----------------------------------------------------------------------------
-- 암호화 메타 (KMS 키 버전 관리)
-- ----------------------------------------------------------------------------
CREATE TABLE tbl_encryption_metadata (
    enc_id BIGINT AUTO_INCREMENT,

    entity_type VARCHAR(30) COMMENT '테이블명',
    entity_id BINARY(16) COMMENT 'PK',

    key_version VARCHAR(32) NOT NULL COMMENT 'KMS 키 버전',
    algorithm VARCHAR(20) DEFAULT 'AES-256-GCM',

    created_at DATETIME(6),

    PRIMARY KEY (enc_id),
    KEY idx_entity (entity_type, entity_id)
) ENGINE=InnoDB COMMENT='암호화 메타';

-- ----------------------------------------------------------------------------
-- 정책
-- ----------------------------------------------------------------------------
CREATE TABLE tbl_default_limit_policy (
    policy_id TINYINT NOT NULL,

    policy_name VARCHAR(64),
    daily_limit BIGINT NOT NULL,
    monthly_limit BIGINT NOT NULL,
    per_tx_limit BIGINT NOT NULL,

    PRIMARY KEY (policy_id)
);

-- ----------------------------------------------------------------------------
-- 사용자 프로필
-- ----------------------------------------------------------------------------
CREATE TABLE tbl_user_profile (
    user_id BINARY(16) NOT NULL,
    policy_id TINYINT,

    ux_mode VARCHAR(20) DEFAULT 'STANDARD',

    PRIMARY KEY (user_id),

    FOREIGN KEY (user_id) REFERENCES tbl_user(user_id),
    FOREIGN KEY (policy_id) REFERENCES tbl_default_limit_policy(policy_id)
);

-- ----------------------------------------------------------------------------
-- 디바이스
-- ----------------------------------------------------------------------------
CREATE TABLE tbl_device (
    device_id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,

    device_fingerprint CHAR(64) NOT NULL,
    platform VARCHAR(20),

    is_trusted BOOLEAN DEFAULT FALSE,
    last_seen_at DATETIME(6),

    created_at DATETIME(6),

    PRIMARY KEY (device_id),
    UNIQUE KEY uk_user_device (user_id, device_fingerprint),

    FOREIGN KEY (user_id) REFERENCES tbl_user(user_id)
);

-- ----------------------------------------------------------------------------
-- 세션
-- ----------------------------------------------------------------------------
CREATE TABLE tbl_session (
    session_id CHAR(36) NOT NULL,
    user_id BINARY(16) NOT NULL,
    device_id BINARY(16),

    expires_at DATETIME(6) NOT NULL,

    created_at DATETIME(6),

    PRIMARY KEY (session_id),

    KEY idx_user (user_id),

    FOREIGN KEY (user_id) REFERENCES tbl_user(user_id)
);

-- ----------------------------------------------------------------------------
-- 생체 인증 (FIDO2)
-- ----------------------------------------------------------------------------
CREATE TABLE tbl_biometric_credential (
    credential_id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    device_id BINARY(16) NOT NULL,

    public_key VARBINARY(1024) NOT NULL,
    sign_count BIGINT DEFAULT 0,

    registered_at DATETIME(6),

    PRIMARY KEY (credential_id),

    FOREIGN KEY (user_id) REFERENCES tbl_user(user_id),
    FOREIGN KEY (device_id) REFERENCES tbl_device(device_id)
);

-- ----------------------------------------------------------------------------
-- 금융기관
-- ----------------------------------------------------------------------------
CREATE TABLE tbl_institution_master (
    institution_code VARCHAR(16) NOT NULL,
    institution_name VARCHAR(64),

    PRIMARY KEY (institution_code)
);

-- ----------------------------------------------------------------------------
-- 연동 기관
-- ----------------------------------------------------------------------------
CREATE TABLE tbl_linked_institution (
    link_id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    institution_code VARCHAR(16) NOT NULL,

    status VARCHAR(20) DEFAULT 'ACTIVE',

    created_at DATETIME(6),

    PRIMARY KEY (link_id),

    FOREIGN KEY (user_id) REFERENCES tbl_user(user_id),
    FOREIGN KEY (institution_code)
        REFERENCES tbl_institution_master(institution_code)
);

-- ----------------------------------------------------------------------------
-- 계좌
-- ----------------------------------------------------------------------------
CREATE TABLE tbl_account (
    account_id BINARY(16) NOT NULL,
    link_id BINARY(16) NOT NULL,

    account_no_hash CHAR(64),
    account_type VARCHAR(20),

    balance BIGINT,

    PRIMARY KEY (account_id),

    FOREIGN KEY (link_id)
        REFERENCES tbl_linked_institution(link_id)
);

-- ----------------------------------------------------------------------------
-- 계좌 스냅샷
-- ----------------------------------------------------------------------------
CREATE TABLE tbl_account_snapshot (
    snapshot_id BIGINT AUTO_INCREMENT,
    account_id BINARY(16) NOT NULL,

    as_of_date DATE NOT NULL,
    balance BIGINT NOT NULL,

    PRIMARY KEY (snapshot_id, as_of_date),
    KEY idx_snapshot_account (account_id, as_of_date)
);

-- ----------------------------------------------------------------------------
-- 이체 주문 (동시성 + idempotency)
-- ----------------------------------------------------------------------------
CREATE TABLE tbl_transfer_order (
    order_id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,

    idempotency_key CHAR(64) NOT NULL,

    from_account_id BINARY(16) NOT NULL,

    amount BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,

    locked_at DATETIME(6),
    lock_version INT DEFAULT 0,

    created_at DATETIME(6),

    PRIMARY KEY (order_id),

    UNIQUE KEY uk_idempotency (idempotency_key),

    KEY idx_user_status (user_id, status),

    FOREIGN KEY (user_id) REFERENCES tbl_user(user_id),
    FOREIGN KEY (from_account_id)
        REFERENCES tbl_account(account_id)
);

-- ----------------------------------------------------------------------------
-- 이체 이벤트
-- ----------------------------------------------------------------------------
CREATE TABLE tbl_transfer_event (
    event_id BIGINT AUTO_INCREMENT,
    order_id BINARY(16) NOT NULL,

    event_type VARCHAR(30),
    payload JSON,

    occurred_at DATETIME(6),

    PRIMARY KEY (event_id),

    KEY idx_order (order_id),

    FOREIGN KEY (order_id)
        REFERENCES tbl_transfer_order(order_id)
);

-- ----------------------------------------------------------------------------
-- 이체 실패
-- ----------------------------------------------------------------------------
CREATE TABLE tbl_transfer_failure (
    failure_id BIGINT AUTO_INCREMENT,
    order_id BINARY(16),

    error_code VARCHAR(50),
    error_message VARCHAR(255),

    failed_at DATETIME(6),

    PRIMARY KEY (failure_id),

    FOREIGN KEY (order_id)
        REFERENCES tbl_transfer_order(order_id)
);

-- ----------------------------------------------------------------------------
-- 이상거래 룰
-- ----------------------------------------------------------------------------
CREATE TABLE tbl_fraud_rule (
    rule_id VARCHAR(32) NOT NULL,
    rule_name VARCHAR(128),
    severity VARCHAR(20),
    action VARCHAR(20),

    PRIMARY KEY (rule_id)
);

-- ----------------------------------------------------------------------------
-- 이상거래 로그
-- ----------------------------------------------------------------------------
CREATE TABLE tbl_fraud_detection_log (
    detection_id BIGINT AUTO_INCREMENT,

    rule_id VARCHAR(32),
    order_id BINARY(16),
    user_id BINARY(16),

    detected_at DATETIME(6),

    PRIMARY KEY (detection_id),

    FOREIGN KEY (rule_id)
        REFERENCES tbl_fraud_rule(rule_id),

    FOREIGN KEY (user_id)
        REFERENCES tbl_user(user_id),

    FOREIGN KEY (order_id)
        REFERENCES tbl_transfer_order(order_id)
);

-- ----------------------------------------------------------------------------
-- Outbox (이벤트 발행)
-- ----------------------------------------------------------------------------
CREATE TABLE tbl_outbox_event (
    event_id BIGINT AUTO_INCREMENT,

    aggregate_type VARCHAR(30),
    aggregate_id BINARY(16),

    event_type VARCHAR(30),
    payload JSON,

    status VARCHAR(20) DEFAULT 'PENDING',

    created_at DATETIME(6),

    PRIMARY KEY (event_id),
    KEY idx_status (status)
);

-- ----------------------------------------------------------------------------
-- 동의 문서
-- ----------------------------------------------------------------------------
CREATE TABLE tbl_consent_document (
    consent_type VARCHAR(20),
    version VARCHAR(16),

    PRIMARY KEY (consent_type, version)
);

-- ----------------------------------------------------------------------------
-- 동의 기록
-- ----------------------------------------------------------------------------
CREATE TABLE tbl_consent_record (
    consent_id BINARY(16),
    user_id BINARY(16),

    consent_type VARCHAR(20),
    consent_version VARCHAR(16),

    agreed_at DATETIME(6),

    PRIMARY KEY (consent_id),

    FOREIGN KEY (user_id) REFERENCES tbl_user(user_id),

    FOREIGN KEY (consent_type, consent_version)
        REFERENCES tbl_consent_document(consent_type, version)
);

-- ----------------------------------------------------------------------------
-- 알림
-- ----------------------------------------------------------------------------
CREATE TABLE tbl_notification (
    notification_id BIGINT AUTO_INCREMENT,

    user_id BINARY(16),
    event_id BIGINT,

    type VARCHAR(30),

    created_at DATETIME(6),

    PRIMARY KEY (notification_id),

    FOREIGN KEY (user_id) REFERENCES tbl_user(user_id),
    FOREIGN KEY (event_id)
        REFERENCES tbl_transfer_event(event_id)
);

-- ----------------------------------------------------------------------------
-- 감사 로그
-- ----------------------------------------------------------------------------
CREATE TABLE tbl_audit_log (
    audit_id BIGINT AUTO_INCREMENT,

    actor_id BINARY(16),
    action VARCHAR(50),

    occurred_at DATETIME(6),

    PRIMARY KEY (audit_id)
);
-- ----------------------------------------------------------------------------
-- BURTY Phase 1: 기준정보 / 현금흐름 / 정책 (added 2026-05-04)
-- ----------------------------------------------------------------------------

-- 기준정보 (lookup catalog) -------------------------------------------------
CREATE TABLE tbl_code (
    code_id          VARCHAR(64)  NOT NULL COMMENT '자연키: GROUP.VALUE',
    code_group       VARCHAR(40)  NOT NULL,
    code_value       VARCHAR(40)  NOT NULL,
    code_name_ko     VARCHAR(80)  NOT NULL,
    code_name_en     VARCHAR(80),
    parent_code_id   VARCHAR(64),
    sort_order       INT          NOT NULL DEFAULT 0,
    use_yn           CHAR(1)      NOT NULL DEFAULT 'Y',
    description      VARCHAR(255),
    attr1            VARCHAR(255),
    attr2            VARCHAR(255),
    attr3            VARCHAR(255),
    attr4            VARCHAR(255),
    attr5            VARCHAR(255),
    effective_from   DATE,
    effective_to     DATE,
    created_at       DATETIME(6) NOT NULL,
    updated_at       DATETIME(6) NOT NULL,
    created_by       VARCHAR(64),
    updated_by       VARCHAR(64),
    PRIMARY KEY (code_id),
    UNIQUE KEY uk_code_group_value (code_group, code_value),
    KEY idx_code_parent (parent_code_id),
    KEY idx_code_group_use_sort (code_group, use_yn, sort_order)
) ENGINE=InnoDB COMMENT='기준정보 카탈로그 (lookup-only)';

-- 현금흐름 일정 ------------------------------------------------------------
CREATE TABLE tbl_cashflow_schedule (
    schedule_id          BINARY(16)  NOT NULL,
    user_id              BINARY(16)  NOT NULL,
    schedule_type_code   VARCHAR(40) NOT NULL COMMENT 'tbl_code.SCHEDULE_TYPE',
    label                VARCHAR(80) NOT NULL,
    amount               BIGINT      NOT NULL,
    direction            VARCHAR(8)  NOT NULL COMMENT 'INCOME/EXPENSE',
    day_of_month         INT         NOT NULL,
    account_id           BINARY(16),
    active               BOOLEAN     NOT NULL DEFAULT TRUE,
    source               VARCHAR(16) NOT NULL DEFAULT 'USER',
    created_at           DATETIME(6) NOT NULL,
    updated_at           DATETIME(6) NOT NULL,
    PRIMARY KEY (schedule_id),
    KEY idx_cf_schedule_user (user_id, active),
    KEY idx_cf_schedule_user_day (user_id, day_of_month)
) ENGINE=InnoDB COMMENT='반복 현금흐름 일정 (월급/월세/카드대금 등)';

-- 반복 지출 (자동 인식) ----------------------------------------------------
CREATE TABLE tbl_recurring_expense (
    recurring_id           BINARY(16)  NOT NULL,
    user_id                BINARY(16)  NOT NULL,
    expense_category_code  VARCHAR(40) NOT NULL COMMENT 'tbl_code.EXPENSE_CATEGORY',
    name                   VARCHAR(80) NOT NULL,
    avg_amount             BIGINT      NOT NULL,
    day_of_month           INT         NOT NULL,
    confidence             DOUBLE,
    occurrence_count       INT,
    last_seen_at           DATETIME(6),
    active                 BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at             DATETIME(6) NOT NULL,
    updated_at             DATETIME(6) NOT NULL,
    PRIMARY KEY (recurring_id),
    KEY idx_recurring_user (user_id),
    KEY idx_recurring_user_category (user_id, expense_category_code)
) ENGINE=InnoDB COMMENT='거래내역 분석으로 인식된 반복 지출';

-- 정책 카탈로그 ------------------------------------------------------------
CREATE TABLE tbl_policy (
    policy_code        VARCHAR(64)  NOT NULL,
    policy_type_code   VARCHAR(40)  NOT NULL COMMENT 'tbl_code.POLICY_TYPE',
    title              VARCHAR(200) NOT NULL,
    support_type       VARCHAR(40),
    age_min            INT,
    age_max            INT,
    income_max         BIGINT,
    occupation_code    VARCHAR(40)  COMMENT 'tbl_code.OCCUPATION_TYPE',
    residence_code     VARCHAR(40)  COMMENT 'tbl_code.RESIDENCE_TYPE',
    benefit_summary    VARCHAR(500),
    apply_url          VARCHAR(500),
    valid_from         DATE,
    valid_to           DATE,
    active             BOOLEAN      NOT NULL DEFAULT TRUE,
    priority_base      INT,
    created_at         DATETIME(6)  NOT NULL,
    updated_at         DATETIME(6)  NOT NULL,
    PRIMARY KEY (policy_code),
    KEY idx_policy_active (active, valid_from, valid_to),
    KEY idx_policy_type (policy_type_code)
) ENGINE=InnoDB COMMENT='청년 정책 카탈로그';

-- ----------------------------------------------------------------------------
-- BURTY Phase 2: 페르소나 / 거래 / 룰 / 추천 / 가족동의 (added 2026-05-04)
-- ----------------------------------------------------------------------------

CREATE TABLE tbl_persona_profile (
    persona_id              BINARY(16)  NOT NULL,
    user_id                 BINARY(16)  NOT NULL,
    occupation_code         VARCHAR(40),
    residence_code          VARCHAR(40),
    household_type          VARCHAR(30),
    monthly_income_avg      BIGINT,
    income_variability_pct  DOUBLE,
    age                     INT,
    source                  VARCHAR(16) NOT NULL DEFAULT 'INFERRED',
    inferred_at             DATETIME(6),
    user_overridden         BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at              DATETIME(6) NOT NULL,
    updated_at              DATETIME(6) NOT NULL,
    PRIMARY KEY (persona_id),
    UNIQUE KEY idx_persona_user (user_id)
) ENGINE=InnoDB COMMENT='사용자 페르소나 프로필';

CREATE TABLE tbl_transaction (
    tx_id                  BINARY(16)  NOT NULL,
    user_id                BINARY(16)  NOT NULL,
    account_id             BINARY(16),
    external_tx_id         VARCHAR(80) NOT NULL,
    txn_date               DATE        NOT NULL,
    amount                 BIGINT      NOT NULL,
    direction              VARCHAR(8)  NOT NULL COMMENT 'IN/OUT',
    merchant               VARCHAR(120),
    memo                   VARCHAR(255),
    expense_category_code  VARCHAR(40),
    income_category_code   VARCHAR(40),
    source                 VARCHAR(16) NOT NULL,
    category_confidence    DOUBLE,
    created_at             DATETIME(6) NOT NULL,
    PRIMARY KEY (tx_id),
    UNIQUE KEY idx_txn_dedup (user_id, external_tx_id),
    KEY idx_txn_user_date (user_id, txn_date),
    KEY idx_txn_user_category (user_id, expense_category_code)
) ENGINE=InnoDB COMMENT='거래내역';

CREATE TABLE tbl_category_rule (
    rule_id                VARCHAR(64)  NOT NULL,
    merchant_pattern       VARCHAR(120) NOT NULL,
    match_type             VARCHAR(16)  NOT NULL DEFAULT 'CONTAINS',
    expense_category_code  VARCHAR(40),
    income_category_code   VARCHAR(40),
    priority               INT          NOT NULL DEFAULT 50,
    source                 VARCHAR(16)  NOT NULL DEFAULT 'SYSTEM',
    active                 BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at             DATETIME(6)  NOT NULL,
    updated_at             DATETIME(6)  NOT NULL,
    PRIMARY KEY (rule_id),
    KEY idx_rule_active_priority (active, priority)
) ENGINE=InnoDB COMMENT='거래 분류 룰';

CREATE TABLE tbl_action_recommendation (
    rec_id                  VARCHAR(80)  NOT NULL,
    action_type_code        VARCHAR(40)  NOT NULL,
    title_template          VARCHAR(200) NOT NULL,
    description_template    VARCHAR(500) NOT NULL,
    base_score              DOUBLE       NOT NULL DEFAULT 50,
    estimated_improvement   BIGINT       NOT NULL DEFAULT 0,
    occupation_code         VARCHAR(40),
    min_min_balance         BIGINT,
    max_min_balance         BIGINT,
    active                  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at              DATETIME(6)  NOT NULL,
    updated_at              DATETIME(6)  NOT NULL,
    PRIMARY KEY (rec_id),
    KEY idx_action_active_score (active, base_score),
    KEY idx_action_persona (occupation_code)
) ENGINE=InnoDB COMMENT='추천 행동 후보 카탈로그';

CREATE TABLE tbl_action_feedback_score (
    score_id          VARCHAR(100) NOT NULL,
    user_id           VARCHAR(64)  NOT NULL,
    action_type_code  VARCHAR(40)  NOT NULL,
    accept_count      INT          NOT NULL DEFAULT 0,
    reject_count      INT          NOT NULL DEFAULT 0,
    score             INT          NOT NULL DEFAULT 0,
    updated_at        DATETIME(6)  NOT NULL,
    PRIMARY KEY (score_id),
    UNIQUE KEY uk_feedback_score_user_action (user_id, action_type_code)
) ENGINE=InnoDB COMMENT='사용자별 행동 피드백 누적 점수';

CREATE TABLE tbl_family_consent (
    consent_pair_id   VARCHAR(200) NOT NULL,
    parent_user_id    VARCHAR(64)  NOT NULL,
    child_user_id     VARCHAR(64)  NOT NULL,
    consented         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,
    PRIMARY KEY (consent_pair_id),
    UNIQUE KEY uk_family_consent_pair (parent_user_id, child_user_id),
    KEY idx_family_consent_parent (parent_user_id)
) ENGINE=InnoDB COMMENT='부모-자녀 동의 (Phase 1 in-memory store DB화)';

-- ----------------------------------------------------------------------------
-- BURTY Phase 3: 이력 / MyData 상태 / 사용자 설정 / 이체 기록 (added 2026-05-04)
-- ----------------------------------------------------------------------------

CREATE TABLE tbl_cashflow_forecast (
    forecast_id          BIGINT      AUTO_INCREMENT,
    user_id              VARCHAR(64) NOT NULL,
    forecast_date        DATE        NOT NULL,
    opening_balance      BIGINT      NOT NULL,
    minimum_balance      BIGINT      NOT NULL,
    risk_date            DATE,
    risk_reason          VARCHAR(500),
    horizon_days         INT         NOT NULL DEFAULT 30,
    actual_min_balance   BIGINT,
    accuracy_pct         DOUBLE,
    created_at           DATETIME(6) NOT NULL,
    PRIMARY KEY (forecast_id),
    UNIQUE KEY uk_forecast_user_date (user_id, forecast_date),
    KEY idx_forecast_user_date (user_id, forecast_date)
) ENGINE=InnoDB COMMENT='30일 현금흐름 예측 일별 스냅샷';

CREATE TABLE tbl_risk_assessment (
    assessment_id      BIGINT      AUTO_INCREMENT,
    user_id            VARCHAR(64) NOT NULL,
    level              VARCHAR(16) NOT NULL,
    threshold_amount   BIGINT      NOT NULL,
    projected_balance  BIGINT      NOT NULL,
    risk_date          DATE,
    reason             VARCHAR(500),
    assessed_at        DATETIME(6) NOT NULL,
    PRIMARY KEY (assessment_id),
    KEY idx_risk_user_assessed (user_id, assessed_at),
    KEY idx_risk_user_level (user_id, level)
) ENGINE=InnoDB COMMENT='위험 진단 이력';

CREATE TABLE tbl_income_pattern (
    pattern_id      BIGINT      AUTO_INCREMENT,
    user_id         VARCHAR(64) NOT NULL,
    period_yyyymm   VARCHAR(7)  NOT NULL,
    total_income    BIGINT      NOT NULL,
    income_count    INT         NOT NULL,
    avg_income      BIGINT,
    stddev          DOUBLE,
    min_income      BIGINT,
    max_income      BIGINT,
    computed_at     DATETIME(6) NOT NULL,
    PRIMARY KEY (pattern_id),
    UNIQUE KEY uk_income_user_month (user_id, period_yyyymm),
    KEY idx_income_user (user_id)
) ENGINE=InnoDB COMMENT='월별 수입 패턴 (변동수입 모델)';

CREATE TABLE tbl_mydata_link_status (
    link_status_id    VARCHAR(200) NOT NULL,
    user_id           VARCHAR(64)  NOT NULL,
    institution_code  VARCHAR(64)  NOT NULL DEFAULT 'MYDATA',
    status            VARCHAR(16)  NOT NULL,
    linked_at         DATETIME(6),
    token_expires_at  DATETIME(6),
    last_error_code   VARCHAR(64),
    last_error_at     DATETIME(6),
    unlinked_at       DATETIME(6),
    created_at        DATETIME(6) NOT NULL,
    updated_at        DATETIME(6) NOT NULL,
    PRIMARY KEY (link_status_id),
    UNIQUE KEY uk_mydata_link_user_inst (user_id, institution_code),
    KEY idx_mydata_link_user (user_id)
) ENGINE=InnoDB COMMENT='MyData 연동 상태/오류 추적';

CREATE TABLE tbl_user_setting (
    setting_id           VARCHAR(200) NOT NULL,
    user_id              VARCHAR(64)  NOT NULL,
    setting_key          VARCHAR(40)  NOT NULL,
    setting_value_long   BIGINT,
    setting_value_str    VARCHAR(500),
    updated_at           DATETIME(6)  NOT NULL,
    PRIMARY KEY (setting_id),
    UNIQUE KEY uk_user_setting_key (user_id, setting_key)
) ENGINE=InnoDB COMMENT='사용자 설정 key-value (이체 한도 등)';

CREATE TABLE tbl_transfer_record (
    transfer_id      VARCHAR(80)  NOT NULL,
    user_id          VARCHAR(64)  NOT NULL,
    from_account     VARCHAR(80),
    to_account       VARCHAR(80),
    amount           BIGINT       NOT NULL,
    status           VARCHAR(20)  NOT NULL,
    family_notified  BOOLEAN      NOT NULL DEFAULT FALSE,
    description      VARCHAR(200),
    created_at       DATETIME(6)  NOT NULL,
    PRIMARY KEY (transfer_id),
    KEY idx_transfer_record_user (user_id, created_at)
) ENGINE=InnoDB COMMENT='Phase 1 in-memory transferStore DB화';

-- ----------------------------------------------------------------------------
-- BURTY Phase 4: 정책 매칭 로그 / 등록 계좌 (added 2026-05-05)
-- ----------------------------------------------------------------------------

CREATE TABLE tbl_policy_match_log (
    match_log_id      BIGINT       AUTO_INCREMENT,
    user_id           VARCHAR(64)  NOT NULL,
    policy_code       VARCHAR(64)  NOT NULL,
    policy_title      VARCHAR(200),
    priority_score    INT          NOT NULL,
    rank_in_match     INT          NOT NULL,
    occupation_code   VARCHAR(40),
    applied           BOOLEAN      NOT NULL DEFAULT FALSE,
    applied_at        DATETIME(6),
    matched_at        DATETIME(6)  NOT NULL,
    PRIMARY KEY (match_log_id),
    KEY idx_policy_match_user (user_id, matched_at),
    KEY idx_policy_match_policy (policy_code)
) ENGINE=InnoDB COMMENT='정책 매칭 이력 (신청률 추적)';

CREATE TABLE tbl_registered_account (
    registered_id     VARCHAR(200) NOT NULL,
    user_id           VARCHAR(64)  NOT NULL,
    account_no        VARCHAR(80)  NOT NULL,
    alias             VARCHAR(80),
    registered_at     DATETIME(6)  NOT NULL,
    PRIMARY KEY (registered_id),
    UNIQUE KEY uk_registered_account_pair (user_id, account_no),
    KEY idx_registered_account_user (user_id)
) ENGINE=InnoDB COMMENT='이체 등록 계좌 (이상거래 판정용)';

-- ----------------------------------------------------------------------------
-- BURTY Phase 5: PII 강화 / SSE / 정책 신청률 (added 2026-05-05)
-- 변경: tbl_registered_account의 account_no를 hash+encrypted+masked 3컬럼으로 분리
-- ----------------------------------------------------------------------------
-- 마이그레이션 노트(운영 DB):
--   ALTER TABLE tbl_registered_account
--     ADD COLUMN account_no_hash CHAR(64),
--     ADD COLUMN account_no_encrypted VARCHAR(500),
--     ADD COLUMN account_no_masked VARCHAR(80);
--   UPDATE tbl_registered_account
--     SET account_no_hash = SHA2(account_no, 256),
--         account_no_masked = CONCAT(REPEAT('*', GREATEST(LENGTH(account_no)-4,4)), RIGHT(account_no, 4))
--     WHERE account_no IS NOT NULL;
--   ALTER TABLE tbl_registered_account DROP INDEX uk_registered_account_pair;
--   ALTER TABLE tbl_registered_account ADD UNIQUE KEY uk_registered_account_pair (user_id, account_no_hash);
--   ALTER TABLE tbl_registered_account DROP COLUMN account_no;
