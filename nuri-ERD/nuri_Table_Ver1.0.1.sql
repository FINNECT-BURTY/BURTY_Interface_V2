-- ----------------------------------------------------------------------------
-- 데이터베이스 생성
-- ----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS nuri
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE nuri;


-- ============================================================================
-- 01. Identity Context (인증·본인확인)
-- ============================================================================

CREATE TABLE tbl_user (
                          user_id             BINARY(16)      NOT NULL COMMENT 'UUID v4',
                          ci_hash             CHAR(64)        NOT NULL COMMENT 'CI 결정적 해시',
                          ci_encrypted        VARBINARY(512)  NOT NULL COMMENT 'CI AES-256-GCM 암호화',
                          phone_hash          CHAR(64)        NOT NULL COMMENT '전화번호 해시',
                          phone_encrypted     VARBINARY(256)  NOT NULL COMMENT '전화번호 암호화',
                          status              ENUM('ACTIVE', 'SUSPENDED', 'WITHDRAWN') NOT NULL DEFAULT 'ACTIVE',
                          last_login_at       DATETIME(6)     NULL,
                          last_login_ip       VARBINARY(16)   NULL COMMENT 'IPv4/IPv6 binary',
                          failed_login_count  INT UNSIGNED    NOT NULL DEFAULT 0,
                          withdrawn_at        DATETIME(6)     NULL,
                          created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                          updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                          PRIMARY KEY (user_id),
                          UNIQUE KEY uk_user_ci_hash (ci_hash),
                          UNIQUE KEY uk_user_phone_hash (phone_hash),
                          KEY idx_user_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 계정';


CREATE TABLE tbl_user_profile (
                                  user_id             BINARY(16)      NOT NULL,
                                  name_encrypted      VARBINARY(256)  NOT NULL COMMENT '이름 암호화',
                                  birthdate_encrypted VARBINARY(64)   NOT NULL COMMENT 'YYYYMMDD 암호화',
                                  age_range           TINYINT UNSIGNED NULL COMMENT '통계용 연령대',
                                  ux_mode             ENUM('SENIOR', 'STANDARD') NOT NULL DEFAULT 'STANDARD',
                                  font_scale          DECIMAL(3,2)    NOT NULL DEFAULT 1.00,
                                  voice_enabled       BOOLEAN         NOT NULL DEFAULT FALSE,
                                  preferences         JSON            NULL,
                                  created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                  updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                                  PRIMARY KEY (user_id),
                                  KEY idx_user_profile_ux_mode (ux_mode),
                                  CONSTRAINT fk_user_profile_user FOREIGN KEY (user_id) REFERENCES tbl_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 프로필';


CREATE TABLE tbl_device (
                            device_id           BINARY(16)      NOT NULL,
                            user_id             BINARY(16)      NOT NULL,
                            device_fingerprint  CHAR(64)        NOT NULL,
                            platform            ENUM('IOS', 'ANDROID', 'WEB') NOT NULL,
                            os_version          VARCHAR(32)     NULL,
                            app_version         VARCHAR(32)     NULL,
                            fcm_token           VARCHAR(512)    NULL,
                            is_trusted          BOOLEAN         NOT NULL DEFAULT FALSE,
                            last_seen_at        DATETIME(6)     NULL,
                            created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                            updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                            revoked_at          DATETIME(6)     NULL,
                            PRIMARY KEY (device_id),
                            UNIQUE KEY uk_device_user_fingerprint (user_id, device_fingerprint),
                            KEY idx_device_user_active (user_id, revoked_at),
                            CONSTRAINT fk_device_user FOREIGN KEY (user_id) REFERENCES tbl_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 디바이스';


CREATE TABLE tbl_biometric_credential (
                                          credential_id       BINARY(16)      NOT NULL,
                                          user_id             BINARY(16)      NOT NULL,
                                          device_id           BINARY(16)      NOT NULL,
                                          credential_type     ENUM('FINGERPRINT', 'FACE_ID', 'PIN') NOT NULL,
                                          public_key          VARBINARY(1024) NOT NULL COMMENT 'FIDO2 공개키',
                                          credential_id_raw   VARBINARY(256)  NOT NULL COMMENT 'WebAuthn credential ID',
                                          sign_count          BIGINT UNSIGNED NOT NULL DEFAULT 0,
                                          aaguid              BINARY(16)      NULL,
                                          registered_at       DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                          last_used_at        DATETIME(6)     NULL,
                                          revoked_at          DATETIME(6)     NULL,
                                          PRIMARY KEY (credential_id),
                                          UNIQUE KEY uk_biometric_raw (credential_id_raw),
                                          KEY idx_biometric_user_device (user_id, device_id, revoked_at),
                                          CONSTRAINT fk_biometric_user FOREIGN KEY (user_id) REFERENCES tbl_user (user_id),
                                          CONSTRAINT fk_biometric_device FOREIGN KEY (device_id) REFERENCES tbl_device (device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='FIDO2 생체인증';


CREATE TABLE tbl_consent_record (
                                    consent_id          BINARY(16)      NOT NULL,
                                    user_id             BINARY(16)      NOT NULL,
                                    consent_type        ENUM('TERMS', 'PRIVACY', 'MYDATA', 'FAMILY_SHARE', 'MARKETING', 'THIRD_PARTY_SHARE') NOT NULL,
                                    consent_version     VARCHAR(16)     NOT NULL,
                                    document_hash       CHAR(64)        NOT NULL,
                                    agreed_at           DATETIME(6)     NOT NULL,
                                    revoked_at          DATETIME(6)     NULL,
                                    revoke_reason       VARCHAR(255)    NULL,
                                    ip_address          VARBINARY(16)   NULL,
                                    user_agent          VARCHAR(512)    NULL,
                                    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                    PRIMARY KEY (consent_id),
                                    KEY idx_consent_user_type (user_id, consent_type, revoked_at),
                                    KEY idx_consent_user_agreed (user_id, agreed_at),
                                    CONSTRAINT fk_consent_user FOREIGN KEY (user_id) REFERENCES tbl_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='동의 이력';


-- ============================================================================
-- 02. Asset Context (자산)
-- ============================================================================

CREATE TABLE tbl_linked_institution (
                                        link_id                 BINARY(16)      NOT NULL,
                                        user_id                 BINARY(16)      NOT NULL,
                                        institution_code        VARCHAR(16)     NOT NULL,
                                        institution_name        VARCHAR(64)     NOT NULL COMMENT '연동 시점 기관명 스냅샷(표시·감사). 최신 표기는 tbl_institution_master',
                                        institution_type        ENUM('BANK', 'CARD', 'SECURITIES', 'PENSION', 'INSURANCE', 'P2P', 'CAPITAL') NOT NULL,
                                        access_token_encrypted  VARBINARY(2048) NOT NULL,
                                        refresh_token_encrypted VARBINARY(2048) NOT NULL,
                                        token_expires_at        DATETIME(6)     NOT NULL,
                                        consent_expires_at      DATETIME(6)     NOT NULL,
                                        status                  ENUM('ACTIVE', 'EXPIRED', 'REVOKED', 'ERROR') NOT NULL DEFAULT 'ACTIVE',
                                        last_synced_at          DATETIME(6)     NULL,
                                        last_error_code         VARCHAR(32)     NULL,
                                        last_error_at           DATETIME(6)     NULL,
                                        created_at              DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                        updated_at              DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                                        PRIMARY KEY (link_id),
                                        UNIQUE KEY uk_link_user_institution (user_id, institution_code),
                                        KEY idx_link_status_expires (status, consent_expires_at),
                                        KEY idx_link_sync (last_synced_at),
                                        CONSTRAINT fk_link_user FOREIGN KEY (user_id) REFERENCES tbl_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='마이데이터 연동 기관';


CREATE TABLE tbl_account (
                             account_id          BINARY(16)      NOT NULL,
                             link_id             BINARY(16)      NOT NULL,
                             account_no_encrypted VARBINARY(256) NOT NULL,
                             account_no_hash     CHAR(64)        NOT NULL,
                             account_no_masked   VARCHAR(32)     NOT NULL COMMENT '예: ***1234',
                             account_name        VARCHAR(64)     NULL,
                             account_type        ENUM('DEPOSIT', 'SAVINGS', 'CHECKING', 'STOCK', 'FUND', 'PENSION', 'LOAN', 'CARD') NOT NULL,
                             currency            CHAR(3)         NOT NULL DEFAULT 'KRW',
                             is_primary          BOOLEAN         NOT NULL DEFAULT FALSE,
                             first_synced_at     DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                             last_balance        BIGINT          NULL COMMENT '원 단위',
                             last_balance_at     DATETIME(6)     NULL,
                             closed_at           DATETIME(6)     NULL,
                             created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                             updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                             PRIMARY KEY (account_id),
                             UNIQUE KEY uk_account_link_hash (link_id, account_no_hash),
                             KEY idx_account_link_type (link_id, account_type, closed_at),
                             CONSTRAINT fk_account_link FOREIGN KEY (link_id) REFERENCES tbl_linked_institution (link_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='계좌 메타';


CREATE TABLE tbl_account_snapshot (
                                      snapshot_id         BIGINT UNSIGNED AUTO_INCREMENT,
                                      account_id          BINARY(16)      NOT NULL,
                                      as_of_date          DATE            NOT NULL,
                                      balance             BIGINT          NOT NULL,
                                      available_balance   BIGINT          NULL,
                                      holdings            JSON            NULL,
                                      captured_at         DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                      PRIMARY KEY (snapshot_id),
                                      UNIQUE KEY uk_snapshot_account_date (account_id, as_of_date),
                                      KEY idx_snapshot_date (as_of_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='일별 자산 스냅샷';


-- ============================================================================
-- 03. Transaction Context (자금이동)
-- ============================================================================

CREATE TABLE tbl_transfer_order (
                                    order_id                BINARY(16)      NOT NULL,
                                    user_id                 BINARY(16)      NOT NULL,
                                    idempotency_key         CHAR(64)        NOT NULL,
                                    from_account_id         BINARY(16)      NOT NULL,
                                    to_account_no_encrypted VARBINARY(256)  NOT NULL,
                                    to_account_no_masked    VARCHAR(32)     NOT NULL,
                                    to_bank_code            VARCHAR(16)     NOT NULL,
                                    to_holder_name          VARCHAR(64)     NULL,
                                    amount                  BIGINT          NOT NULL,
                                    memo                    VARCHAR(255)    NULL,
                                    purpose                 ENUM('SELF', 'TRANSFER', 'PAYMENT', 'INVESTMENT') NOT NULL DEFAULT 'TRANSFER',
                                    status                  ENUM('PENDING', 'AUTH_REQUESTED', 'AUTHORIZED', 'EXECUTING', 'EXECUTED', 'FAILED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
                                    biometric_credential_id BINARY(16)      NULL,
                                    bank_transaction_id     VARCHAR(64)     NULL,
                                    scheduled_at            DATETIME(6)     NULL,
                                    executed_at             DATETIME(6)     NULL,
                                    failed_reason           VARCHAR(255)    NULL,
                                    created_at              DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                    updated_at              DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                                    PRIMARY KEY (order_id),
                                    UNIQUE KEY uk_transfer_idempotency (idempotency_key),
                                    KEY idx_transfer_user_status (user_id, status, created_at),
                                    KEY idx_transfer_user_created (user_id, created_at DESC),
                                    KEY idx_transfer_scheduled (status, scheduled_at),
                                    KEY idx_transfer_account (from_account_id, created_at),
                                    CONSTRAINT fk_transfer_user FOREIGN KEY (user_id) REFERENCES tbl_user (user_id),
                                    CONSTRAINT fk_transfer_from_account FOREIGN KEY (from_account_id) REFERENCES tbl_account (account_id),
                                    CONSTRAINT fk_transfer_credential FOREIGN KEY (biometric_credential_id) REFERENCES tbl_biometric_credential (credential_id),
                                    CONSTRAINT chk_transfer_amount_positive CHECK (amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='이체 주문';


CREATE TABLE tbl_transfer_event (
                                    event_id            BIGINT UNSIGNED AUTO_INCREMENT,
                                    order_id            BINARY(16)      NOT NULL,
                                    sequence_no         INT UNSIGNED    NOT NULL,
                                    event_type          ENUM('CREATED', 'AUTH_REQUESTED', 'AUTH_APPROVED', 'AUTH_REJECTED', 'BANK_CALLED', 'BANK_RESPONDED', 'EXECUTED', 'FAILED', 'CANCELLED', 'RETRIED') NOT NULL,
                                    payload             JSON            NULL,
                                    actor_type          ENUM('USER', 'SYSTEM', 'AI_AGENT', 'BANK', 'SCHEDULER') NOT NULL,
                                    actor_id            BINARY(16)      NULL,
                                    occurred_at         DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                    PRIMARY KEY (event_id),
                                    UNIQUE KEY uk_event_order_seq (order_id, sequence_no),
                                    KEY idx_event_order_time (order_id, occurred_at),
                                    KEY idx_event_type_time (event_type, occurred_at),
                                    CONSTRAINT fk_event_order FOREIGN KEY (order_id) REFERENCES tbl_transfer_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='이체 이벤트 - Event Sourcing';


CREATE TABLE tbl_spending_limit (
                                    limit_id            BINARY(16)      NOT NULL,
                                    user_id             BINARY(16)      NOT NULL,
                                    period_type         ENUM('DAILY', 'MONTHLY', 'PER_TRANSACTION') NOT NULL,
                                    amount_limit        BIGINT          NOT NULL,
                                    effective_from      DATETIME(6)     NOT NULL,
                                    effective_to        DATETIME(6)     NULL,
                                    changed_by          ENUM('USER', 'GUARDIAN', 'SYSTEM', 'COMPLIANCE') NOT NULL,
                                    change_reason       VARCHAR(255)    NULL,
                                    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                    PRIMARY KEY (limit_id),
                                    KEY idx_limit_user_effective (user_id, period_type, effective_from, effective_to),
                                    CONSTRAINT fk_limit_user FOREIGN KEY (user_id) REFERENCES tbl_user (user_id),
                                    CONSTRAINT chk_limit_amount_positive CHECK (amount_limit > 0),
                                    CONSTRAINT chk_limit_effective_range CHECK (effective_to IS NULL OR effective_to >= effective_from)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='이체 한도';


CREATE TABLE tbl_daily_transfer_usage (
                                          user_id             BINARY(16)      NOT NULL,
                                          usage_date          DATE            NOT NULL,
                                          total_amount        BIGINT          NOT NULL DEFAULT 0,
                                          transfer_count      INT UNSIGNED    NOT NULL DEFAULT 0,
                                          updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                                          PRIMARY KEY (user_id, usage_date),
                                          CONSTRAINT fk_usage_user FOREIGN KEY (user_id) REFERENCES tbl_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='일일 이체 사용량';


-- ============================================================================
-- 04. Report Context (Easy-Read 월간 리포트)
-- ============================================================================

CREATE TABLE tbl_monthly_report (
                                    report_id               BINARY(16)      NOT NULL,
                                    user_id                 BINARY(16)      NOT NULL,
                                    period_month            DATE            NOT NULL COMMENT 'YYYY-MM-01',
                                    status                  ENUM('GENERATING', 'READY', 'DELIVERED', 'FAILED', 'ARCHIVED') NOT NULL DEFAULT 'GENERATING',
                                    pdf_object_key          VARCHAR(512)    NULL COMMENT 'S3 경로',
                                    pdf_size_bytes          INT UNSIGNED    NULL,
                                    share_token             CHAR(32)        NULL,
                                    share_expires_at        DATETIME(6)     NULL,
                                    llm_invocation_id       BINARY(16)      NULL,
                                    total_cost_usd          DECIMAL(10, 6)  NULL,
                                    generated_at            DATETIME(6)     NULL,
                                    delivered_at            DATETIME(6)     NULL,
                                    failed_reason           VARCHAR(255)    NULL,
                                    created_at              DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                    updated_at              DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                                    PRIMARY KEY (report_id),
                                    UNIQUE KEY uk_report_user_period (user_id, period_month),
                                    UNIQUE KEY uk_report_share_token (share_token),
                                    KEY idx_report_status_created (status, created_at),
                                    CONSTRAINT fk_report_user FOREIGN KEY (user_id) REFERENCES tbl_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='월간 Easy-Read 리포트';


CREATE TABLE tbl_report_section (
                                    section_id          BINARY(16)      NOT NULL,
                                    report_id           BINARY(16)      NOT NULL,
                                    section_type        ENUM('SUMMARY', 'SPENDING', 'ASSETS', 'WARNING', 'ACTION', 'ACHIEVEMENT') NOT NULL,
                                    display_order       TINYINT UNSIGNED NOT NULL,
                                    easy_read_text      TEXT            NOT NULL COMMENT '비유 변환 결과',
                                    raw_data            JSON            NOT NULL COMMENT '원본 수치',
                                    traffic_light       ENUM('GREEN', 'YELLOW', 'RED') NOT NULL DEFAULT 'GREEN',
                                    action_label        VARCHAR(64)     NULL,
                                    action_payload      JSON            NULL,
                                    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                    PRIMARY KEY (section_id),
                                    KEY idx_section_report_order (report_id, display_order),
                                    CONSTRAINT fk_section_report FOREIGN KEY (report_id) REFERENCES tbl_monthly_report (report_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='리포트 섹션';


-- ============================================================================
-- 05. Family Context (가족 공유)
-- ============================================================================

CREATE TABLE tbl_guardian_link (
                                   link_id                 BINARY(16)      NOT NULL,
                                   senior_user_id          BINARY(16)      NOT NULL COMMENT '보호받는 부모',
                                   guardian_user_id        BINARY(16)      NOT NULL COMMENT '자녀',
                                   relation                ENUM('CHILD', 'SPOUSE', 'PARENT', 'SIBLING', 'CAREGIVER') NOT NULL,
                                   permission              ENUM('VIEW_ONLY', 'VIEW_AND_ALERT') NOT NULL DEFAULT 'VIEW_ONLY',
                                   senior_consent_id       BINARY(16)      NOT NULL,
                                   guardian_consent_id     BINARY(16)      NOT NULL,
                                   status                  ENUM('PENDING', 'ACTIVE', 'SUSPENDED', 'REVOKED') NOT NULL DEFAULT 'PENDING',
                                   linked_at               DATETIME(6)     NULL,
                                   revoked_at              DATETIME(6)     NULL,
                                   revoked_by              ENUM('SENIOR', 'GUARDIAN', 'SYSTEM') NULL,
                                   created_at              DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                   updated_at              DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                                   PRIMARY KEY (link_id),
                                   UNIQUE KEY uk_guardian_pair (senior_user_id, guardian_user_id),
                                   KEY idx_guardian_senior (senior_user_id, status),
                                   KEY idx_guardian_guardian (guardian_user_id, status),
                                   CONSTRAINT fk_guardian_senior FOREIGN KEY (senior_user_id) REFERENCES tbl_user (user_id),
                                   CONSTRAINT fk_guardian_guardian FOREIGN KEY (guardian_user_id) REFERENCES tbl_user (user_id),
                                   CONSTRAINT fk_guardian_senior_consent FOREIGN KEY (senior_consent_id) REFERENCES tbl_consent_record (consent_id),
                                   CONSTRAINT fk_guardian_guardian_consent FOREIGN KEY (guardian_consent_id) REFERENCES tbl_consent_record (consent_id),
                                   CONSTRAINT chk_guardian_not_self CHECK (senior_user_id <> guardian_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='가족 연결';


CREATE TABLE tbl_alert_subscription (
                                        subscription_id     BINARY(16)      NOT NULL,
                                        link_id             BINARY(16)      NOT NULL,
                                        alert_type          ENUM('TRANSFER_OVER_AMOUNT', 'LATE_NIGHT_TRANSACTION', 'UNUSUAL_PATTERN', 'LOGIN_NEW_DEVICE', 'CONSENT_EXPIRING', 'BALANCE_BELOW_THRESHOLD') NOT NULL,
                                        threshold_amount    BIGINT          NULL,
                                        threshold_time_from TIME            NULL,
                                        threshold_time_to   TIME            NULL,
                                        channel             ENUM('PUSH', 'SMS', 'EMAIL', 'ALL') NOT NULL DEFAULT 'PUSH',
                                        is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
                                        created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                        updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                                        PRIMARY KEY (subscription_id),
                                        UNIQUE KEY uk_sub_link_type (link_id, alert_type),
                                        KEY idx_sub_active (is_active, alert_type),
                                        CONSTRAINT fk_sub_link FOREIGN KEY (link_id) REFERENCES tbl_guardian_link (link_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='알림 구독';


-- ============================================================================
-- 06. Notification Context
-- ============================================================================

CREATE TABLE tbl_notification (
                                  notification_id     BIGINT UNSIGNED AUTO_INCREMENT,
                                  recipient_user_id   BINARY(16)      NOT NULL,
                                  notification_type   ENUM('TRANSFER_ALERT', 'REPORT_READY', 'CONSENT_EXPIRING', 'GUARDIAN_REQUEST', 'BIOMETRIC_REGISTERED', 'UNUSUAL_LOGIN', 'SYSTEM') NOT NULL,
                                  channel             ENUM('PUSH', 'SMS', 'EMAIL', 'IN_APP') NOT NULL,
                                  title               VARCHAR(128)    NOT NULL,
                                  body                TEXT            NOT NULL,
                                  deep_link           VARCHAR(512)    NULL,
                                  related_entity_type VARCHAR(32)     NULL,
                                  related_entity_id   BINARY(16)      NULL,
                                  status              ENUM('QUEUED', 'SENT', 'DELIVERED', 'READ', 'FAILED') NOT NULL DEFAULT 'QUEUED',
                                  sent_at             DATETIME(6)     NULL,
                                  delivered_at        DATETIME(6)     NULL,
                                  read_at             DATETIME(6)     NULL,
                                  failed_reason       VARCHAR(255)    NULL,
                                  created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                  PRIMARY KEY (notification_id),
                                  KEY idx_notification_user_created (recipient_user_id, created_at DESC),
                                  KEY idx_notification_status (status, created_at),
                                  CONSTRAINT fk_notification_recipient_user FOREIGN KEY (recipient_user_id) REFERENCES tbl_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='알림 발송 이력';


-- ============================================================================
-- 07. Audit Context (감사)
-- ============================================================================

CREATE TABLE tbl_audit_log (
                               audit_id            BIGINT UNSIGNED AUTO_INCREMENT,
                               occurred_at         DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                               actor_type          ENUM('USER', 'GUARDIAN', 'SYSTEM', 'AI_AGENT', 'BANK', 'ADMIN', 'SCHEDULER') NOT NULL,
                               actor_id            BINARY(16)      NULL,
                               target_type         VARCHAR(64)     NOT NULL,
                               target_id           BINARY(16)      NULL,
                               action              VARCHAR(64)     NOT NULL,
                               result              ENUM('SUCCESS', 'FAILED', 'DENIED') NOT NULL,
                               ip_address          VARBINARY(16)   NULL,
                               user_agent          VARCHAR(512)    NULL,
                               request_id          CHAR(36)        NULL,
                               session_id          CHAR(36)        NULL,
                               before_snapshot     JSON            NULL,
                               after_snapshot      JSON            NULL,
                               metadata            JSON            NULL,
                               error_code          VARCHAR(32)     NULL,
                               error_message       VARCHAR(512)    NULL,
                               PRIMARY KEY (audit_id),
                               KEY idx_audit_actor (actor_type, actor_id, occurred_at),
                               KEY idx_audit_target (target_type, target_id, occurred_at),
                               KEY idx_audit_action_result (action, result, occurred_at),
                               KEY idx_audit_request (request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='감사 로그';


-- ============================================================================
-- 08. Master Data (마스터 데이터)
-- ============================================================================

CREATE TABLE tbl_institution_master (
                                        institution_code    VARCHAR(16)     NOT NULL,
                                        institution_name    VARCHAR(64)     NOT NULL,
                                        institution_type    ENUM('BANK', 'CARD', 'SECURITIES', 'PENSION', 'INSURANCE', 'P2P', 'CAPITAL') NOT NULL,
                                        api_base_url        VARCHAR(255)    NULL,
                                        logo_url            VARCHAR(255)    NULL,
                                        is_mydata_supported BOOLEAN         NOT NULL DEFAULT TRUE,
                                        is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
                                        priority_order      SMALLINT        NOT NULL DEFAULT 999,
                                        created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                        updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                                        PRIMARY KEY (institution_code),
                                        KEY idx_institution_type_active (institution_type, is_active, priority_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='금융기관 마스터';


CREATE TABLE tbl_consent_document (
                                      consent_type        ENUM('TERMS', 'PRIVACY', 'MYDATA', 'FAMILY_SHARE', 'MARKETING', 'THIRD_PARTY_SHARE') NOT NULL,
                                      version             VARCHAR(16)     NOT NULL,
                                      title               VARCHAR(128)    NOT NULL,
                                      document_url        VARCHAR(512)    NOT NULL,
                                      document_hash       CHAR(64)        NOT NULL,
                                      is_mandatory        BOOLEAN         NOT NULL DEFAULT TRUE,
                                      effective_from      DATETIME(6)     NOT NULL,
                                      effective_to        DATETIME(6)     NULL,
                                      created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                      PRIMARY KEY (consent_type, version),
                                      KEY idx_consent_doc_effective (effective_from, effective_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='약관 문서 버전';


CREATE TABLE tbl_default_limit_policy (
                                          policy_id           TINYINT         NOT NULL,
                                          policy_name         VARCHAR(64)     NOT NULL,
                                          age_range_from      TINYINT         NULL,
                                          age_range_to        TINYINT         NULL,
                                          daily_limit         BIGINT          NOT NULL,
                                          monthly_limit       BIGINT          NOT NULL,
                                          per_tx_limit        BIGINT          NOT NULL,
                                          description         VARCHAR(255)    NULL,
                                          PRIMARY KEY (policy_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='기본 한도 정책';


CREATE TABLE tbl_fraud_rule (
                                rule_id             VARCHAR(32)     NOT NULL,
                                rule_name           VARCHAR(128)    NOT NULL,
                                rule_description    TEXT            NULL,
                                condition_json      JSON            NOT NULL,
                                severity            ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') NOT NULL,
                                action              ENUM('LOG', 'NOTIFY', 'BLOCK', 'REQUIRE_2FA') NOT NULL,
                                is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
                                created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                                PRIMARY KEY (rule_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='이상거래 탐지 규칙';


-- ============================================================================
-- 09. Deferred FKs & integrity (참조 순서상 테이블 생성 후 부여)
-- ============================================================================

ALTER TABLE tbl_consent_record
    ADD CONSTRAINT fk_consent_document
        FOREIGN KEY (consent_type, consent_version)
        REFERENCES tbl_consent_document (consent_type, version);

ALTER TABLE tbl_linked_institution
    ADD CONSTRAINT fk_link_institution_master
        FOREIGN KEY (institution_code)
        REFERENCES tbl_institution_master (institution_code);


-- 동일 사용자·기간 유형에서 유효 구간이 겹치지 않도록 DB에서 차단 (NULL effective_to = 무기한 종료)
DROP TRIGGER IF EXISTS tr_spending_limit_overlap_bi;
DROP TRIGGER IF EXISTS tr_spending_limit_overlap_bu;

DELIMITER $$

CREATE TRIGGER tr_spending_limit_overlap_bi
    BEFORE INSERT ON tbl_spending_limit
    FOR EACH ROW
BEGIN
    IF EXISTS (
        SELECT 1
        FROM tbl_spending_limit s
        WHERE s.user_id = NEW.user_id
          AND s.period_type = NEW.period_type
          AND NOT (
                COALESCE(s.effective_to, '9999-12-31 23:59:59.999999') < NEW.effective_from
             OR COALESCE(NEW.effective_to, '9999-12-31 23:59:59.999999') < s.effective_from
          )
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'tbl_spending_limit: 동일 user_id·period_type에서 effective_from~effective_to 구간이 기존 행과 겹칩니다.';
    END IF;
END$$

CREATE TRIGGER tr_spending_limit_overlap_bu
    BEFORE UPDATE ON tbl_spending_limit
    FOR EACH ROW
BEGIN
    IF EXISTS (
        SELECT 1
        FROM tbl_spending_limit s
        WHERE s.user_id = NEW.user_id
          AND s.period_type = NEW.period_type
          AND s.limit_id <> NEW.limit_id
          AND NOT (
                COALESCE(s.effective_to, '9999-12-31 23:59:59.999999') < NEW.effective_from
             OR COALESCE(NEW.effective_to, '9999-12-31 23:59:59.999999') < s.effective_from
          )
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'tbl_spending_limit: 동일 user_id·period_type에서 effective_from~effective_to 구간이 다른 행과 겹칩니다.';
    END IF;
END$$

DELIMITER ;