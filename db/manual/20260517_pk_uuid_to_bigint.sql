-- Convert all UUID/BINARY(16) primary keys (and their FK columns) to BIGINT AUTO_INCREMENT.
-- Run AFTER 20260517_user_id_auto_increment.sql (which already migrated tbl_user.user_id).
--
-- PRE-CONDITIONS:
--   * Target tables must be EMPTY. UUID → BIGINT conversion is not data-preserving.
--     If any rows exist, run `TRUNCATE` on them first (FK-aware order) or build an
--     explicit UUID→BIGINT mapping table.
--   * tbl_user_session also has session_id changed from BINARY(16) to BIGINT AUTO_INCREMENT,
--     which is what the latest code expects.
--   * tbl_biometric_credential.aaguid stays UUID — it is a WebAuthn standard identifier.
--   * tbl_monthly_report.llm_invocation_id stays UUID — it is an external LLM trace id.
--
-- Verify on each environment:
--   SELECT COUNT(*) FROM <table>;  -- expect 0 before running
--   SELECT TABLE_NAME, CONSTRAINT_NAME
--   FROM information_schema.KEY_COLUMN_USAGE
--   WHERE TABLE_SCHEMA='burty' AND CONSTRAINT_NAME LIKE 'FK%';
--   -- adjust FK drop names below if they differ in your environment.

USE burty;

-- ============================================================================
-- 1. Drop FK constraints that reference the PKs we are about to rewrite.
--    FK names below are best-effort defaults; replace if information_schema shows different names.
-- ============================================================================

-- account FKs
ALTER TABLE tbl_transfer_order DROP FOREIGN KEY IF EXISTS FK_tbl_transfer_order_from_account;
-- consent FKs
-- (none in current schema beyond user FK)
-- device FKs
ALTER TABLE tbl_biometric_credential DROP FOREIGN KEY IF EXISTS FK_tbl_biometric_credential_device;
-- guardian_link FKs
ALTER TABLE tbl_alert_subscription DROP FOREIGN KEY IF EXISTS FK_tbl_alert_subscription_link;
-- linked_institution FKs
ALTER TABLE tbl_account DROP FOREIGN KEY IF EXISTS FK_tbl_account_link;
-- monthly_report FKs
ALTER TABLE tbl_report_section DROP FOREIGN KEY IF EXISTS FK_tbl_report_section_report;
-- transfer_order FKs
ALTER TABLE tbl_transfer_event DROP FOREIGN KEY IF EXISTS FK_tbl_transfer_event_order;
-- biometric_credential FKs (referenced by transfer_order)
ALTER TABLE tbl_transfer_order DROP FOREIGN KEY IF EXISTS FK_tbl_transfer_order_biometric_credential;

-- ============================================================================
-- 2. Truncate every affected table so the type rewrite is safe.
--    Disable FK checks temporarily.
-- ============================================================================

SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE tbl_alert_subscription;
TRUNCATE TABLE tbl_report_section;
TRUNCATE TABLE tbl_transfer_event;
TRUNCATE TABLE tbl_transfer_order;
TRUNCATE TABLE tbl_biometric_credential;
TRUNCATE TABLE tbl_account;
TRUNCATE TABLE tbl_linked_institution;
TRUNCATE TABLE tbl_cashflow_schedule;
TRUNCATE TABLE tbl_recurring_expense;
TRUNCATE TABLE tbl_persona_profile;
TRUNCATE TABLE tbl_consent_record;
TRUNCATE TABLE tbl_device;
TRUNCATE TABLE tbl_guardian_link;
TRUNCATE TABLE tbl_monthly_report;
TRUNCATE TABLE tbl_spending_limit;
TRUNCATE TABLE tbl_transaction;
TRUNCATE TABLE tbl_user_session;
TRUNCATE TABLE tbl_notification;
TRUNCATE TABLE tbl_audit_log;
TRUNCATE TABLE tbl_transfer_usage_daily;

-- ============================================================================
-- 3. Rewrite primary keys to BIGINT AUTO_INCREMENT.
-- ============================================================================

ALTER TABLE tbl_account            MODIFY account_id      BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE tbl_alert_subscription MODIFY subscription_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE tbl_biometric_credential MODIFY credential_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE tbl_cashflow_schedule  MODIFY schedule_id     BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE tbl_consent_record     MODIFY consent_id      BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE tbl_device             MODIFY device_id       BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE tbl_guardian_link      MODIFY link_id         BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE tbl_linked_institution MODIFY link_id         BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE tbl_monthly_report     MODIFY report_id       BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE tbl_persona_profile    MODIFY persona_id      BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE tbl_recurring_expense  MODIFY recurring_id    BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE tbl_report_section     MODIFY section_id      BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE tbl_spending_limit     MODIFY limit_id        BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE tbl_transaction        MODIFY tx_id           BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE tbl_transfer_order     MODIFY order_id        BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE tbl_user_session       MODIFY session_id      BIGINT NOT NULL AUTO_INCREMENT;

-- ============================================================================
-- 4. Rewrite FK / lookup columns that referenced those PKs.
--    (user_id columns are already BIGINT from the prior migration.)
-- ============================================================================

-- cashflow_schedule
ALTER TABLE tbl_cashflow_schedule  MODIFY user_id    BIGINT NOT NULL;
ALTER TABLE tbl_cashflow_schedule  MODIFY account_id BIGINT NULL;
-- persona_profile
ALTER TABLE tbl_persona_profile    MODIFY user_id    BIGINT NOT NULL;
-- recurring_expense
ALTER TABLE tbl_recurring_expense  MODIFY user_id    BIGINT NOT NULL;
-- transaction
ALTER TABLE tbl_transaction        MODIFY user_id    BIGINT NOT NULL;
ALTER TABLE tbl_transaction        MODIFY account_id BIGINT NULL;
-- guardian_link consent refs
ALTER TABLE tbl_guardian_link      MODIFY senior_consent_id   BIGINT NOT NULL;
ALTER TABLE tbl_guardian_link      MODIFY guardian_consent_id BIGINT NOT NULL;
-- transfer_event actor
ALTER TABLE tbl_transfer_event     MODIFY actor_id   BIGINT NULL;
-- notification related entity (polymorphic)
ALTER TABLE tbl_notification       MODIFY related_entity_id BIGINT NULL;
-- audit_log actor/target
ALTER TABLE tbl_audit_log          MODIFY actor_id   BIGINT NULL;
ALTER TABLE tbl_audit_log          MODIFY target_id  BIGINT NULL;
-- daily transfer usage user
ALTER TABLE tbl_transfer_usage_daily MODIFY user_id  BIGINT NOT NULL;

-- ============================================================================
-- 5. Re-create FK constraints (with explicit names so they are stable).
-- ============================================================================

ALTER TABLE tbl_account
    ADD CONSTRAINT FK_tbl_account_link FOREIGN KEY (link_id) REFERENCES tbl_linked_institution(link_id);
ALTER TABLE tbl_alert_subscription
    ADD CONSTRAINT FK_tbl_alert_subscription_link FOREIGN KEY (link_id) REFERENCES tbl_guardian_link(link_id);
ALTER TABLE tbl_biometric_credential
    ADD CONSTRAINT FK_tbl_biometric_credential_device FOREIGN KEY (device_id) REFERENCES tbl_device(device_id);
ALTER TABLE tbl_report_section
    ADD CONSTRAINT FK_tbl_report_section_report FOREIGN KEY (report_id) REFERENCES tbl_monthly_report(report_id);
ALTER TABLE tbl_transfer_order
    ADD CONSTRAINT FK_tbl_transfer_order_from_account FOREIGN KEY (from_account_id) REFERENCES tbl_account(account_id);
ALTER TABLE tbl_transfer_order
    ADD CONSTRAINT FK_tbl_transfer_order_biometric_credential FOREIGN KEY (biometric_credential_id) REFERENCES tbl_biometric_credential(credential_id);
ALTER TABLE tbl_transfer_event
    ADD CONSTRAINT FK_tbl_transfer_event_order FOREIGN KEY (order_id) REFERENCES tbl_transfer_order(order_id);

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================================
-- 6. Sanity check (run manually, do not include in script).
-- ============================================================================
-- SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, COLUMN_TYPE, EXTRA
-- FROM information_schema.COLUMNS
-- WHERE TABLE_SCHEMA = 'burty'
--   AND COLUMN_NAME IN ('account_id','subscription_id','credential_id','schedule_id','consent_id',
--                       'device_id','link_id','report_id','persona_id','recurring_id','section_id',
--                       'limit_id','tx_id','order_id','session_id','user_id','related_entity_id',
--                       'actor_id','target_id','event_id','notification_id','audit_id');
