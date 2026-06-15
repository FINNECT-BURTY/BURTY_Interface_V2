-- 마이데이터 전송요구·동의·감사로그 (직접 등록 대비)

CREATE TABLE IF NOT EXISTS tbl_mydata_transmission_request (
    request_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    institution_code VARCHAR(64) NOT NULL,
    scope VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL,
    requested_at DATETIME NOT NULL,
    authorized_at DATETIME NULL,
    revoked_at DATETIME NULL,
    consent_expires_at DATETIME NULL,
    INDEX idx_md_tx_req_user (user_id),
    INDEX idx_md_tx_req_inst (user_id, institution_code)
);

CREATE TABLE IF NOT EXISTS tbl_mydata_consent_history (
    consent_history_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transmission_request_id BIGINT NULL,
    user_id VARCHAR(64) NOT NULL,
    institution_code VARCHAR(64) NOT NULL,
    scope VARCHAR(500) NOT NULL,
    consent_version VARCHAR(20) NOT NULL,
    agreed_at DATETIME NOT NULL,
    revoked_at DATETIME NULL,
    revoke_reason VARCHAR(200) NULL,
    INDEX idx_md_consent_user (user_id)
);

CREATE TABLE IF NOT EXISTS tbl_mydata_transmission_log (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    institution_code VARCHAR(64) NULL,
    action VARCHAR(64) NOT NULL,
    direction VARCHAR(16) NOT NULL,
    summary VARCHAR(1000) NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_md_tx_log_user (user_id, created_at)
);
