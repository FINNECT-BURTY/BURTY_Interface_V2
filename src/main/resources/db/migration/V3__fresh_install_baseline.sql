-- BURTY 스키마 부트스트랩 (신규 환경 전용) — 자동 생성 파일
--
-- 생성: ./gradlew test --tests com.burty.SchemaDumpTests && python3 tools/generate_baseline.py
--
-- 목적
--   ddl-auto=validate 환경에서 빈 데이터베이스가 부팅 가능하도록 전체 스키마를 만든다.
--   예전에는 V1 이 주석 한 줄뿐이라 신규 환경이 아예 뜨지 못했고, 스키마의 진짜 원본은
--   손으로 관리하는 db/burty_Table_Ver1.1.sql 이었다 (이미 낡아 있었다).
--
-- 안전성
--   모든 문장이 IF NOT EXISTS 이고 UNIQUE/FK 는 CREATE TABLE 안에 인라인되어 있다.
--   따라서 테이블이 이미 있는 기존 데이터베이스에서는 이 마이그레이션 전체가 no-op 이며,
--   기존 제약을 중복 생성하지 않는다.
--
-- 주의
--   이 파일을 손으로 고치지 말 것. 스키마 변경은 새 V 마이그레이션으로 추가한다.
--   (Flyway 는 적용된 마이그레이션의 체크섬을 검증하므로 수정 시 기존 환경이 깨진다.)

SET FOREIGN_KEY_CHECKS = 0;

create table if not exists tbl_account (
    currency varchar(3) not null, is_primary bit not null, account_id bigint not null auto_increment, closed_at datetime(6), first_synced_at datetime(6) not null, last_balance bigint, last_balance_at datetime(6), link_id bigint not null, account_no_hash varchar(64) not null, account_no varchar(80) not null, account_no_masked varchar(80) not null, account_name varchar(255), account_type varchar(20) not null, primary key (account_id),
    constraint fk_account_link_id foreign key (link_id) references tbl_linked_institution (link_id)
) engine=InnoDB;

create table if not exists tbl_account_snapshot (
    as_of_date date not null, account_id bigint not null, available_balance bigint, balance bigint not null, captured_at datetime(6) not null, snapshot_id bigint not null auto_increment, holdings JSON, primary key (snapshot_id),
    constraint fk_account_snapshot_account_id foreign key (account_id) references tbl_account (account_id)
) engine=InnoDB;

create table if not exists tbl_action_execution (
    executed bit not null, executed_at datetime(6) not null, execution_id bigint not null auto_increment, action_type varchar(64) not null, user_id varchar(64) not null, message varchar(255), primary key (execution_id)
) engine=InnoDB;

create table if not exists tbl_action_feedback (
    created_at datetime(6) not null, feedback_id bigint not null auto_increment, action_type varchar(64) not null, user_id varchar(64) not null, feedback varchar(100) not null, primary key (feedback_id)
) engine=InnoDB;

create table if not exists tbl_action_feedback_score (
    accept_count integer not null, reject_count integer not null, score integer not null, updated_at datetime(6) not null, action_type_code varchar(40) not null, user_id varchar(64) not null, score_id varchar(100) not null, primary key (score_id),
    constraint uk_action_feedback_score_user_id_action_type_code unique (user_id, action_type_code)
) engine=InnoDB;

create table if not exists tbl_action_recommendation (
    active bit not null, base_score float(53) not null, created_at datetime(6) not null, estimated_improvement bigint not null, max_min_balance bigint, min_min_balance bigint, updated_at datetime(6) not null, action_type_code varchar(40) not null, occupation_code varchar(40), rec_id varchar(80) not null, title_template varchar(200) not null, description_template varchar(500) not null, primary key (rec_id)
) engine=InnoDB;

create table if not exists tbl_admin_user (
    admin_id bigint not null auto_increment, created_at datetime(6) not null, updated_at datetime(6) not null, username varchar(50) not null, password_hash varchar(255) not null, role varchar(20) not null, status varchar(20) not null, primary key (admin_id),
    constraint uk_admin_user_username unique (username)
) engine=InnoDB;

create table if not exists tbl_ai_fallback_template (
    active bit not null, updated_at datetime(6) not null, risk_level varchar(20), cause_type varchar(40), occupation_code varchar(40), template_key varchar(80) not null, template_text varchar(1000) not null, primary key (template_key)
) engine=InnoDB;

create table if not exists tbl_alert_subscription (
    is_active bit not null, threshold_time_from time(0), threshold_time_to time(0), created_at datetime(6) not null, link_id bigint not null, subscription_id bigint not null auto_increment, threshold_amount bigint, updated_at datetime(6) not null, alert_type varchar(23) not null, channel varchar(20) not null, primary key (subscription_id),
    constraint fk_alert_subscription_link_id foreign key (link_id) references tbl_guardian_link (link_id)
) engine=InnoDB;

create table if not exists tbl_audit_log (
    actor_id bigint, audit_id bigint not null auto_increment, occurred_at datetime(6) not null, target_id bigint, request_id varchar(36), session_id varchar(36), action varchar(255) not null, after_snapshot JSON, before_snapshot JSON, error_code varchar(255), error_message varchar(255), metadata JSON, target_type varchar(255) not null, user_agent varchar(255), ip_address varbinary(255), actor_type varchar(20) not null, result varchar(20) not null, primary key (audit_id)
) engine=InnoDB;

create table if not exists tbl_biometric_credential (
    credential_id bigint not null auto_increment, device_id bigint not null, last_used_at datetime(6), registered_at datetime(6) not null, revoked_at datetime(6), sign_count bigint not null, user_id bigint not null, aaguid BINARY(16), credential_id_raw varbinary(255) not null, public_key varbinary(255) not null, credential_type varchar(20) not null, primary key (credential_id),
    constraint fk_biometric_credential_device_id foreign key (device_id) references tbl_device (device_id),
    constraint fk_biometric_credential_user_id foreign key (user_id) references tbl_user (user_id)
) engine=InnoDB;

create table if not exists tbl_cashflow_forecast (
    accuracy_pct float(53), forecast_date date not null, horizon_days integer not null, risk_date date, actual_min_balance bigint, created_at datetime(6) not null, forecast_id bigint not null auto_increment, minimum_balance bigint not null, opening_balance bigint not null, user_id varchar(64) not null, risk_reason varchar(500), primary key (forecast_id),
    constraint uk_cashflow_forecast_user_id_forecast_date unique (user_id, forecast_date)
) engine=InnoDB;

create table if not exists tbl_cashflow_schedule (
    active bit not null, day_of_month integer not null, account_id bigint, amount bigint not null, created_at datetime(6) not null, direction varchar(8) not null, schedule_id bigint not null auto_increment, updated_at datetime(6) not null, user_id bigint not null, source varchar(16) not null, schedule_type_code varchar(40) not null, label varchar(80) not null, primary key (schedule_id)
) engine=InnoDB;

create table if not exists tbl_category_rule (
    active bit not null, priority integer not null, created_at datetime(6) not null, updated_at datetime(6) not null, match_type varchar(16) not null, source varchar(16) not null, expense_category_code varchar(40), income_category_code varchar(40), rule_id varchar(64) not null, merchant_pattern varchar(120) not null, primary key (rule_id)
) engine=InnoDB;

create table if not exists tbl_code (
    effective_from date, effective_to date, sort_order integer not null, use_yn varchar(1) not null, created_at datetime(6) not null, updated_at datetime(6) not null, code_group varchar(40) not null, code_value varchar(40) not null, code_id varchar(64) not null, created_by varchar(64), parent_code_id varchar(64), updated_by varchar(64), code_name_en varchar(80), code_name_ko varchar(80) not null, attr1 varchar(255), attr2 varchar(255), attr3 varchar(255), attr4 varchar(255), attr5 varchar(255), description varchar(255), primary key (code_id),
    constraint uk_code_code_group_code_value unique (code_group, code_value)
) engine=InnoDB;

create table if not exists tbl_consent_record (
    agreed_at datetime(6) not null, consent_id bigint not null auto_increment, revoked_at datetime(6), user_id bigint not null, document_hash varchar(64) not null, consent_version varchar(255) not null, revoke_reason varchar(255), user_agent varchar(255), ip_address varbinary(255), consent_type varchar(21) not null, primary key (consent_id),
    constraint fk_consent_record_user_id foreign key (user_id) references tbl_user (user_id)
) engine=InnoDB;

create table if not exists tbl_daily_transfer_usage (
    transfer_count integer not null, usage_date date not null, total_amount bigint not null, updated_at datetime(6) not null, user_id bigint not null, primary key (usage_date, user_id)
) engine=InnoDB;

create table if not exists tbl_device (
    is_trusted bit not null, created_at datetime(6) not null, device_id bigint not null auto_increment, last_seen_at datetime(6), revoked_at datetime(6), updated_at datetime(6) not null, user_id bigint not null, device_fingerprint varchar(64) not null, device_token_hash varchar(64) not null, device_name varchar(100), device_token varchar(500) not null, app_version varchar(255), fcm_token varchar(255), os_version varchar(255), platform varchar(20) not null, primary key (device_id),
    constraint uk_device_device_token_hash unique (device_token_hash),
    constraint fk_device_user_id foreign key (user_id) references tbl_user (user_id)
) engine=InnoDB;

create table if not exists tbl_family_consent (
    consented bit not null, created_at datetime(6) not null, updated_at datetime(6) not null, child_user_id varchar(64) not null, parent_user_id varchar(64) not null, consent_pair_id varchar(200) not null, primary key (consent_pair_id),
    constraint uk_family_consent_parent_user_id_child_user_id unique (parent_user_id, child_user_id)
) engine=InnoDB;

create table if not exists tbl_guardian_link (
    guardian_consent_id bigint not null, guardian_user_id bigint not null, link_id bigint not null auto_increment, linked_at datetime(6), revoked_at datetime(6), senior_consent_id bigint not null, senior_user_id bigint not null, permission varchar(20) not null, relation varchar(20) not null, revoked_by varchar(20), status varchar(20) not null, primary key (link_id),
    constraint fk_guardian_link_guardian_user_id foreign key (guardian_user_id) references tbl_user (user_id),
    constraint fk_guardian_link_senior_user_id foreign key (senior_user_id) references tbl_user (user_id)
) engine=InnoDB;

create table if not exists tbl_income_pattern (
    income_count integer not null, stddev float(53), period_yyyymm varchar(7) not null, avg_income bigint, computed_at datetime(6) not null, max_income bigint, min_income bigint, pattern_id bigint not null auto_increment, total_income bigint not null, user_id varchar(64) not null, primary key (pattern_id),
    constraint uk_income_pattern_user_id_period_yyyymm unique (user_id, period_yyyymm)
) engine=InnoDB;

create table if not exists tbl_linked_institution (
    consent_expires_at datetime(6) not null, last_error_at datetime(6), last_synced_at datetime(6), link_id bigint not null auto_increment, token_expires_at datetime(6) not null, user_id bigint not null, access_token varchar(2000) not null, refresh_token varchar(2000) not null, institution_code varchar(255) not null, institution_name varchar(255) not null, last_error_code varchar(255), institution_type varchar(20) not null, status varchar(20) not null, primary key (link_id),
    constraint fk_linked_institution_user_id foreign key (user_id) references tbl_user (user_id)
) engine=InnoDB;

create table if not exists tbl_monthly_report (
    pdf_size_bytes integer, period_month date not null, total_cost_usd decimal(10,6), delivered_at datetime(6), generated_at datetime(6), report_id bigint not null auto_increment, share_expires_at datetime(6), user_id bigint not null, llm_invocation_id BINARY(16), share_token varchar(32), failed_reason varchar(255), pdf_object_key varchar(255), status varchar(20) not null, primary key (report_id),
    constraint fk_monthly_report_user_id foreign key (user_id) references tbl_user (user_id)
) engine=InnoDB;

create table if not exists tbl_mydata_consent_history (
    agreed_at datetime(6) not null, consent_history_id bigint not null auto_increment, revoked_at datetime(6), transmission_request_id bigint, consent_version varchar(20) not null, institution_code varchar(64) not null, user_id varchar(64) not null, revoke_reason varchar(200), scope varchar(500) not null, primary key (consent_history_id)
) engine=InnoDB;

create table if not exists tbl_mydata_link_status (
    created_at datetime(6) not null, last_error_at datetime(6), linked_at datetime(6), token_expires_at datetime(6), unlinked_at datetime(6), updated_at datetime(6) not null, status varchar(16) not null, institution_code varchar(64) not null, last_error_code varchar(64), user_id varchar(64) not null, link_status_id varchar(200) not null, primary key (link_status_id),
    constraint uk_mydata_link_status_user_id_institution_code unique (user_id, institution_code)
) engine=InnoDB;

create table if not exists tbl_mydata_transmission_log (
    created_at datetime(6) not null, log_id bigint not null auto_increment, action varchar(64) not null, institution_code varchar(64), user_id varchar(64) not null, summary varchar(1000), direction varchar(20) not null, primary key (log_id)
) engine=InnoDB;

create table if not exists tbl_mydata_transmission_request (
    authorized_at datetime(6), consent_expires_at datetime(6), request_id bigint not null auto_increment, requested_at datetime(6) not null, revoked_at datetime(6), institution_code varchar(64) not null, user_id varchar(64) not null, scope varchar(500) not null, status varchar(20) not null, primary key (request_id)
) engine=InnoDB;

create table if not exists tbl_notification (
    delivered_at datetime(6), notification_id bigint not null auto_increment, read_at datetime(6), recipient_user_id bigint not null, related_entity_id bigint, sent_at datetime(6), body TEXT not null, deep_link varchar(255), failed_reason varchar(255), related_entity_type varchar(255), title varchar(255) not null, channel varchar(20) not null, notification_type varchar(20) not null, status varchar(20) not null, primary key (notification_id),
    constraint fk_notification_recipient_user_id foreign key (recipient_user_id) references tbl_user (user_id)
) engine=InnoDB;

create table if not exists tbl_oauth_state (
    created_at datetime(6) not null, expires_at datetime(6) not null, provider varchar(20) not null, state_key varchar(128) not null, frontend_origin varchar(255), primary key (state_key)
) engine=InnoDB;

create table if not exists tbl_outbox_event (
    attempts integer not null, created_at datetime(6) not null, event_id bigint not null auto_increment, next_attempt_at datetime(6) not null, published_at datetime(6), aggregate_id varchar(64) not null, aggregate_type varchar(64) not null, event_type varchar(64) not null, last_error varchar(500), payload tinytext not null, status varchar(20) not null, primary key (event_id)
) engine=InnoDB;

create table if not exists tbl_persona_profile (
    age integer, income_variability_pct float(53), user_overridden bit not null, created_at datetime(6) not null, inferred_at datetime(6), monthly_income_avg bigint, persona_id bigint not null auto_increment, updated_at datetime(6) not null, user_id bigint not null, source varchar(16) not null, household_type varchar(30), occupation_code varchar(40), residence_code varchar(40), primary key (persona_id),
    constraint uk_persona_profile_user_id unique (user_id)
) engine=InnoDB;

create table if not exists tbl_policy (
    active bit not null, age_max integer, age_min integer, priority_base integer, valid_from date, valid_to date, created_at datetime(6) not null, income_max bigint, updated_at datetime(6) not null, occupation_code varchar(40), policy_type_code varchar(40) not null, residence_code varchar(40), support_type varchar(40), policy_code varchar(64) not null, title varchar(200) not null, apply_url varchar(500), benefit_summary varchar(500), primary key (policy_code)
) engine=InnoDB;

create table if not exists tbl_policy_match_log (
    applied bit not null, priority_score integer not null, rank_in_match integer not null, applied_at datetime(6), match_log_id bigint not null auto_increment, matched_at datetime(6) not null, occupation_code varchar(40), policy_code varchar(64) not null, user_id varchar(64) not null, policy_title varchar(200), primary key (match_log_id)
) engine=InnoDB;

create table if not exists tbl_recurring_expense (
    active bit not null, confidence float(53), day_of_month integer not null, occurrence_count integer, avg_amount bigint not null, created_at datetime(6) not null, last_seen_at datetime(6), recurring_id bigint not null auto_increment, updated_at datetime(6) not null, user_id bigint not null, expense_category_code varchar(40) not null, name varchar(80) not null, primary key (recurring_id)
) engine=InnoDB;

create table if not exists tbl_registered_account (
    registered_at datetime(6) not null, user_id varchar(64) not null, account_no varchar(80) not null, alias varchar(80), registered_id varchar(200) not null, primary key (registered_id),
    constraint uk_registered_account_user_id_account_no unique (user_id, account_no)
) engine=InnoDB;

create table if not exists tbl_report_section (
    display_order integer not null, created_at datetime(6) not null, report_id bigint not null, section_id bigint not null auto_increment, action_label varchar(255), action_payload JSON, easy_read_text TEXT not null, raw_data JSON not null, section_type varchar(20) not null, traffic_light varchar(20) not null, primary key (section_id),
    constraint fk_report_section_report_id foreign key (report_id) references tbl_monthly_report (report_id)
) engine=InnoDB;

create table if not exists tbl_risk_assessment (
    risk_date date, assessed_at datetime(6) not null, assessment_id bigint not null auto_increment, projected_balance bigint not null, threshold_amount bigint not null, level varchar(16) not null, user_id varchar(64) not null, reason varchar(500), primary key (assessment_id)
) engine=InnoDB;

create table if not exists tbl_social_account (
    last_login_at datetime(6), linked_at datetime(6) not null, social_account_id bigint not null auto_increment, user_id bigint not null, provider varchar(20) not null, email_hash varchar(64), provider_user_id_hash varchar(64) not null, display_name varchar(100), email varchar(255), primary key (social_account_id),
    constraint uk_social_account_provider_provider_user_id_hash unique (provider, provider_user_id_hash)
) engine=InnoDB;

create table if not exists tbl_spending_limit (
    amount_limit bigint not null, effective_from datetime(6) not null, effective_to datetime(6), limit_id bigint not null auto_increment, user_id bigint not null, change_reason varchar(255), changed_by varchar(20) not null, period_type varchar(20) not null, primary key (limit_id),
    constraint fk_spending_limit_user_id foreign key (user_id) references tbl_user (user_id)
) engine=InnoDB;

create table if not exists tbl_transaction (
    category_confidence float(53), txn_date date not null, account_id bigint, amount bigint not null, created_at datetime(6) not null, direction varchar(8) not null, tx_id bigint not null auto_increment, user_id bigint not null, source varchar(16) not null, expense_category_code varchar(40), income_category_code varchar(40), external_tx_id varchar(80) not null, merchant varchar(120), memo varchar(255), primary key (tx_id),
    constraint uk_transaction_user_id_external_tx_id unique (user_id, external_tx_id)
) engine=InnoDB;

create table if not exists tbl_transfer_event (
    sequence_no integer not null, actor_id bigint, event_id bigint not null auto_increment, occurred_at datetime(6) not null, order_id bigint not null, payload JSON, actor_type varchar(20) not null, event_type varchar(20) not null, primary key (event_id),
    constraint fk_transfer_event_order_id foreign key (order_id) references tbl_transfer_order (order_id)
) engine=InnoDB;

create table if not exists tbl_transfer_order (
    reconcile_attempts integer not null, amount bigint not null, biometric_credential_id bigint, executed_at datetime(6), from_account_id bigint, next_reconcile_at datetime(6), order_id bigint not null auto_increment, requested_at datetime(6) not null, scheduled_at datetime(6), user_id bigint not null, idempotency_key varchar(64) not null, to_account_no varchar(80) not null, to_account_no_masked varchar(80) not null, bank_transaction_id varchar(255), failed_reason varchar(255), memo varchar(255), to_bank_code varchar(255) not null, to_holder_name varchar(255), purpose varchar(20) not null, status varchar(20) not null, primary key (order_id),
    constraint uk_transfer_order_user_id_idempotency_key unique (user_id, idempotency_key),
    constraint fk_transfer_order_biometric_credential_id foreign key (biometric_credential_id) references tbl_biometric_credential (credential_id),
    constraint fk_transfer_order_from_account_id foreign key (from_account_id) references tbl_account (account_id),
    constraint fk_transfer_order_user_id foreign key (user_id) references tbl_user (user_id)
) engine=InnoDB;

create table if not exists tbl_transfer_record (
    family_notified bit not null, amount bigint not null, created_at datetime(6) not null, status varchar(20) not null, idempotency_key varchar(64), user_id varchar(64) not null, from_account varchar(80), to_account varchar(80), transfer_id varchar(80) not null, description varchar(200), primary key (transfer_id),
    constraint uk_transfer_record_user_id_idempotency_key unique (user_id, idempotency_key)
) engine=InnoDB;

create table if not exists tbl_user (
    failed_login_count integer not null, created_at datetime(6) not null, last_login_at datetime(6), updated_at datetime(6) not null, user_id bigint not null auto_increment, withdrawn_at datetime(6), phone varchar(20) not null, ci_hash varchar(64) not null, phone_hash varchar(64) not null, ci varchar(255) not null, last_login_ip varbinary(255), status varchar(20) not null, primary key (user_id),
    constraint uk_user_ci_hash unique (ci_hash),
    constraint uk_user_phone_hash unique (phone_hash)
) engine=InnoDB;

create table if not exists tbl_user_profile (
    age_range integer, birthdate date not null, font_scale decimal(3,2) not null, voice_enabled bit not null, created_at datetime(6) not null, updated_at datetime(6) not null, user_id bigint not null, name varchar(100) not null, preferences JSON, ux_mode varchar(20) not null, primary key (user_id),
    constraint fk_user_profile_user_id foreign key (user_id) references tbl_user (user_id)
) engine=InnoDB;

create table if not exists tbl_user_session (
    created_at datetime(6) not null, expires_at datetime(6) not null, revoked_at datetime(6), session_id bigint not null auto_increment, user_id bigint not null, device_id varchar(64), refresh_token_hash varchar(64) not null, primary key (session_id)
) engine=InnoDB;

create table if not exists tbl_user_setting (
    setting_value_long bigint, updated_at datetime(6) not null, setting_key varchar(40) not null, user_id varchar(64) not null, setting_id varchar(200) not null, setting_value_str varchar(500), primary key (setting_id),
    constraint uk_user_setting_user_id_setting_key unique (user_id, setting_key)
) engine=InnoDB;

create table if not exists tbl_youth_policy (
    sprt_trgt_age_lmt_yn varchar(1), id bigint not null auto_increment, synced_at datetime(6) not null, aply_prd_se_cd varchar(10), earn_cnd_se_cd varchar(10), mrg_stts_cd varchar(10), sprt_trgt_max_age varchar(10), sprt_trgt_min_age varchar(10), biz_prd_bgng_ymd varchar(20), biz_prd_end_ymd varchar(20), inq_cnt varchar(20), earn_max_amt varchar(30), earn_min_amt varchar(30), frst_reg_dt varchar(30), last_mdfcn_dt varchar(30), plcy_no varchar(50) not null, job_cd varchar(100), lclsf_nm varchar(100), mclsf_nm varchar(100), s_biz_cd varchar(100), school_cd varchar(100), aply_ymd varchar(200), oper_inst_cd_nm varchar(200), sprvsn_inst_cd_nm varchar(200), zip_cd varchar(200), aply_url_addr varchar(500), plcy_kywd_nm varchar(500), plcy_nm varchar(500), ref_url_addr1 varchar(500), earn_etc_cn TEXT, plcy_aply_mthd_cn TEXT, plcy_expln_cn TEXT, plcy_sprt_cn TEXT, primary key (id),
    constraint uk_youth_policy_plcy_no unique (plcy_no)
) engine=InnoDB;


SET FOREIGN_KEY_CHECKS = 1;

create index if not exists idx_action_active_score on tbl_action_recommendation (active, base_score);
create index if not exists idx_action_persona on tbl_action_recommendation (occupation_code);
create index if not exists idx_forecast_user_date on tbl_cashflow_forecast (user_id, forecast_date);
create index if not exists idx_cf_schedule_user on tbl_cashflow_schedule (user_id, active);
create index if not exists idx_cf_schedule_user_day on tbl_cashflow_schedule (user_id, day_of_month);
create index if not exists idx_rule_active_priority on tbl_category_rule (active, priority);
create index if not exists idx_code_parent on tbl_code (parent_code_id);
create index if not exists idx_code_group_use_sort on tbl_code (code_group, use_yn, sort_order);
create index if not exists idx_device_user_fingerprint on tbl_device (user_id, device_fingerprint);
create index if not exists idx_family_consent_parent on tbl_family_consent (parent_user_id);
create index if not exists idx_income_user on tbl_income_pattern (user_id);
create index if not exists idx_md_consent_user on tbl_mydata_consent_history (user_id);
create index if not exists idx_mydata_link_user on tbl_mydata_link_status (user_id);
create index if not exists idx_md_tx_log_user on tbl_mydata_transmission_log (user_id, created_at);
create index if not exists idx_md_tx_req_user on tbl_mydata_transmission_request (user_id);
create index if not exists idx_md_tx_req_inst on tbl_mydata_transmission_request (user_id, institution_code);
create index if not exists idx_oauth_state_expires on tbl_oauth_state (expires_at);
create index if not exists idx_outbox_dispatch on tbl_outbox_event (status, next_attempt_at);
create index if not exists idx_outbox_aggregate on tbl_outbox_event (aggregate_type, aggregate_id);
create index if not exists idx_policy_active on tbl_policy (active, valid_from, valid_to);
create index if not exists idx_policy_type on tbl_policy (policy_type_code);
create index if not exists idx_policy_match_user on tbl_policy_match_log (user_id, matched_at);
create index if not exists idx_policy_match_policy on tbl_policy_match_log (policy_code);
create index if not exists idx_recurring_user on tbl_recurring_expense (user_id);
create index if not exists idx_recurring_user_category on tbl_recurring_expense (user_id, expense_category_code);
create index if not exists idx_registered_account_user on tbl_registered_account (user_id);
create index if not exists idx_risk_user_assessed on tbl_risk_assessment (user_id, assessed_at);
create index if not exists idx_risk_user_level on tbl_risk_assessment (user_id, level);
create index if not exists idx_social_user on tbl_social_account (user_id);
create index if not exists idx_social_email on tbl_social_account (email_hash);
create index if not exists idx_txn_user_date on tbl_transaction (user_id, txn_date);
create index if not exists idx_txn_user_category on tbl_transaction (user_id, expense_category_code);
create index if not exists idx_transfer_order_user on tbl_transfer_order (user_id, order_id);
create index if not exists idx_transfer_order_reconcile on tbl_transfer_order (status, next_reconcile_at);
create index if not exists idx_transfer_record_user on tbl_transfer_record (user_id, created_at);
create index if not exists idx_session_user_active on tbl_user_session (user_id, revoked_at);
create index if not exists idx_session_refresh on tbl_user_session (refresh_token_hash);
create index if not exists idx_youth_policy_lclsf on tbl_youth_policy (lclsf_nm);
create index if not exists idx_youth_policy_zip on tbl_youth_policy (zip_cd);
