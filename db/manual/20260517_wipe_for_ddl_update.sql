-- ============================================================================
-- BURTY 도메인 테이블 전체 DROP — ddl-auto=update 가 entity 정의대로 깨끗하게
-- 재생성하도록 만드는 "wipe" 스크립트.
--
-- WHEN TO USE:
--   * UUID(BINARY 16) → BIGINT PK 마이그레이션 도중 schema 가 중간 상태로 꼬였을 때
--   * 운영 데이터가 모두 폐기 가능한 상태일 때 (스테이징/베타 단계)
--   * 어떤 FK 도 수동으로 떨궈주고 싶지 않을 때
--
-- PRE-CONDITIONS:
--   * 데이터 전부 폐기해도 됨을 확인. 보존 필요한 행이 있으면 dump 먼저.
--   * 이 스크립트는 burty 도메인 테이블만 건드림. flyway/liquibase/Spring/JPA
--     auto-generated 메타 테이블이 있다면 별도.
--
-- AFTER:
--   * 애플리케이션 재기동 → Hibernate 가 entity 정의(BIGINT AUTO_INCREMENT)
--     대로 모든 테이블을 신규 생성.
--   * BaseCodeSeeder/PolicySeeder/CategoryRuleSeeder 등이 정적 데이터 재시드.
--
-- 이전 SQL (20260517_user_id_auto_increment.sql, 20260517_pk_uuid_to_bigint.sql)
-- 은 데이터 보존이 필요한 케이스용. ddl-update 로 굴리는 환경이면 이 wipe 만으로 충분.
-- ============================================================================

USE burty;

SET FOREIGN_KEY_CHECKS = 0;

-- FK 의존성 깊은 순서대로 (어차피 FK_CHECKS=0 이라 순서 무관하지만 가독성).
DROP TABLE IF EXISTS tbl_alert_subscription;
DROP TABLE IF EXISTS tbl_report_section;
DROP TABLE IF EXISTS tbl_transfer_event;
DROP TABLE IF EXISTS tbl_transfer_order;
DROP TABLE IF EXISTS tbl_transfer_record;
DROP TABLE IF EXISTS tbl_biometric_credential;
DROP TABLE IF EXISTS tbl_account_snapshot;
DROP TABLE IF EXISTS tbl_account;
DROP TABLE IF EXISTS tbl_registered_account;
DROP TABLE IF EXISTS tbl_linked_institution;
DROP TABLE IF EXISTS tbl_mydata_link_status;
DROP TABLE IF EXISTS tbl_cashflow_schedule;
DROP TABLE IF EXISTS tbl_cashflow_forecast;
DROP TABLE IF EXISTS tbl_recurring_expense;
DROP TABLE IF EXISTS tbl_income_pattern;
DROP TABLE IF EXISTS tbl_persona_profile;
DROP TABLE IF EXISTS tbl_consent_record;
DROP TABLE IF EXISTS tbl_family_consent;
DROP TABLE IF EXISTS tbl_device;
DROP TABLE IF EXISTS tbl_guardian_link;
DROP TABLE IF EXISTS tbl_monthly_report;
DROP TABLE IF EXISTS tbl_spending_limit;
DROP TABLE IF EXISTS tbl_transaction;
DROP TABLE IF EXISTS tbl_user_session;
DROP TABLE IF EXISTS tbl_notification;
DROP TABLE IF EXISTS tbl_audit_log;
DROP TABLE IF EXISTS tbl_daily_transfer_usage;
DROP TABLE IF EXISTS tbl_policy_match_log;
DROP TABLE IF EXISTS tbl_policy;
DROP TABLE IF EXISTS tbl_action_execution;
DROP TABLE IF EXISTS tbl_action_feedback;
DROP TABLE IF EXISTS tbl_action_feedback_score;
DROP TABLE IF EXISTS tbl_action_recommendation;
DROP TABLE IF EXISTS tbl_ai_fallback_template;
DROP TABLE IF EXISTS tbl_category_rule;
DROP TABLE IF EXISTS tbl_risk_assessment;
DROP TABLE IF EXISTS tbl_code;
DROP TABLE IF EXISTS tbl_oauth_state;
DROP TABLE IF EXISTS tbl_social_account;
DROP TABLE IF EXISTS tbl_user_setting;
DROP TABLE IF EXISTS tbl_user_profile;
DROP TABLE IF EXISTS tbl_user;

-- 혹시 누락된 burty 도메인 테이블이 있는지 확인용 (실행 후 수동으로 확인 권장):
-- SELECT TABLE_NAME FROM information_schema.TABLES
-- WHERE TABLE_SCHEMA = 'burty' AND TABLE_NAME LIKE 'tbl_%';

SET FOREIGN_KEY_CHECKS = 1;
