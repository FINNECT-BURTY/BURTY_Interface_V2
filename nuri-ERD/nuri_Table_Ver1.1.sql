-- ----------------------------------------------------------------------------
-- DATABASE
-- ----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS nuri
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE nuri;

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