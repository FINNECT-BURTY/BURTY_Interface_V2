-- ============================================================================
-- USE CASE 07. AI 에이전트 대화 기록 (MongoDB 컬렉션 조회)
-- ----------------------------------------------------------------------------
-- 실제 대화·메시지·LLM 호출은 MongoDB에 저장되지만,
-- 여기서는 MariaDB에 가벼운 메타 테이블을 두는 경우의 패턴을 함께 제시
-- (초기 MVP에서 단일 DB로 시작할 때 유용)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 7.1 [MongoDB] 최근 대화 10개 목록 (앱 내 대화 탭)
-- ----------------------------------------------------------------------------
-- MongoDB 쿼리 (참고용)
/*
db.tbl_conversation.find({
    user_id: BinData(3, "...base64..."),
    archived_at: null
}).sort({ last_message_at: -1 }).limit(10);

// 인덱스: { user_id: 1, archived_at: 1, last_message_at: -1 }
*/


-- ----------------------------------------------------------------------------
-- 7.2 [MongoDB] 특정 대화의 메시지 이력 (페이징)
-- ----------------------------------------------------------------------------
/*
db.tbl_message.find({
    conversation_id: BinData(3, "..."),
    created_at: { $lt: ISODate("2026-04-21T00:00:00Z") }  // 페이징 커서
}).sort({ created_at: -1 }).limit(50);

// 인덱스: { conversation_id: 1, created_at: -1 }
*/


-- ----------------------------------------------------------------------------
-- 7.3 [MongoDB] LLM 호출 비용 집계 (일별)
-- ----------------------------------------------------------------------------
/*
db.tbl_llm_invocation.aggregate([
    {
        $match: {
            invoked_at: {
                $gte: ISODate("2026-04-01T00:00:00Z"),
                $lt: ISODate("2026-05-01T00:00:00Z")
            }
        }
    },
    {
        $group: {
            _id: {
                date: { $dateToString: { format: "%Y-%m-%d", date: "$invoked_at" } },
                model: "$model"
            },
            call_count: { $sum: 1 },
            total_prompt_tokens: { $sum: "$prompt_tokens" },
            total_completion_tokens: { $sum: "$completion_tokens" },
            total_cost_usd: { $sum: "$cost_usd" },
            avg_latency_ms: { $avg: "$latency_ms" }
        }
    },
    { $sort: { "_id.date": -1, "_id.model": 1 } }
]);
*/


-- ============================================================================
-- USE CASE 08. 감사·규제 대응 (AUDIT LOG)
-- ----------------------------------------------------------------------------
-- 시나리오 1: 금감원 감사 요청 "사용자 X의 이체 이력 전체 제출"
-- 시나리오 2: 보이스피싱 피해 신고 → 해당 이체 과정 완전 재현
-- 시나리오 3: 사용자 개인정보 접근 로그 제출 (GDPR/개인정보보호법)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 8.1 특정 사용자의 전체 이체 이력 (법적 제출용)
-- ----------------------------------------------------------------------------
-- 파티션 프루닝 위해 기간 조건 필수
SELECT
    BIN_TO_UUID(o.order_id, 1) AS order_id,
    o.created_at,
    o.amount,
    li.institution_name AS from_institution,
    a.account_no_masked AS from_account,
    o.to_bank_code,
    o.to_account_no_masked,
    o.to_holder_name,
    o.status,
    o.bank_transaction_id,
    o.executed_at,
    o.failed_reason,
    -- 인증 증빙
    CASE WHEN o.biometric_credential_id IS NOT NULL
         THEN (SELECT credential_type FROM tbl_biometric_credential
               WHERE credential_id = o.biometric_credential_id)
         ELSE 'NONE'
    END AS auth_method,
    -- 이벤트 수
    (SELECT COUNT(*) FROM tbl_transfer_event e WHERE e.order_id = o.order_id)
        AS event_count
FROM tbl_transfer_order o
INNER JOIN tbl_account a ON a.account_id = o.from_account_id
INNER JOIN tbl_linked_institution li ON li.link_id = a.link_id
WHERE o.user_id = ?
  AND o.created_at BETWEEN ? AND ?
ORDER BY o.created_at DESC;


-- ----------------------------------------------------------------------------
-- 8.2 특정 이체의 완전한 재현 (forensic trail)
-- ----------------------------------------------------------------------------
-- 보이스피싱 신고 접수 시: 이체가 어떤 경로로 어떤 인증을 거쳐 실행됐는지 완전 재현
SELECT
    BIN_TO_UUID(o.order_id, 1) AS order_id,
    o.created_at AS order_created_at,
    o.amount,
    o.to_holder_name,
    o.to_account_no_masked,
    o.status AS final_status,

    e.sequence_no,
    e.event_type,
    e.actor_type,
    BIN_TO_UUID(e.actor_id, 1) AS actor_id,
    e.occurred_at,
    e.payload,

    -- 인증 상세
    bc.credential_type AS biometric_type,
    d.platform AS device_platform,
    d.os_version AS device_os,
    INET6_NTOA(u.last_login_ip) AS last_login_ip
FROM tbl_transfer_order o
INNER JOIN tbl_transfer_event e ON e.order_id = o.order_id
LEFT JOIN tbl_biometric_credential bc ON bc.credential_id = o.biometric_credential_id
LEFT JOIN tbl_device d ON d.device_id = bc.device_id
LEFT JOIN tbl_user u ON u.user_id = o.user_id
WHERE o.order_id = ?
ORDER BY e.sequence_no;


-- ----------------------------------------------------------------------------
-- 8.3 사용자 동의 이력 제출 (개인정보보호법 열람권 대응)
-- ----------------------------------------------------------------------------
-- "3년 전에 내가 어떤 약관에 동의했는지 알고 싶어요"
SELECT
    BIN_TO_UUID(consent_id, 1) AS consent_id,
    consent_type,
    consent_version,
    document_hash,
    agreed_at,
    revoked_at,
    revoke_reason,
    INET6_NTOA(ip_address) AS agreed_from_ip,
    user_agent
FROM tbl_consent_record
WHERE user_id = ?
ORDER BY agreed_at DESC;


-- ----------------------------------------------------------------------------
-- 8.4 개인정보 변경 이력 조회 (tbl_user_profile은 현재값만 가짐)
-- ----------------------------------------------------------------------------
-- 이력 관리가 필요한 경우 AUDIT LOG(ClickHouse)에서 조회하지만
-- MariaDB 내에서도 updated_at 기준 간이 조회 가능
SELECT
    BIN_TO_UUID(up.user_id, 1) AS user_id,
    up.ux_mode,
    up.font_scale,
    up.voice_enabled,
    up.updated_at
FROM tbl_user_profile up
WHERE up.user_id = ?;
-- 실제 변경 이력은 ClickHouse의 tbl_audit_log.prop_changes 참조


-- ----------------------------------------------------------------------------
-- 8.5 사용자의 모든 가족 연결 이력 (revoked 포함)
-- ----------------------------------------------------------------------------
SELECT
    BIN_TO_UUID(gl.link_id, 1) AS link_id,
    CASE
        WHEN gl.senior_user_id = ? THEN 'SENIOR'
        ELSE 'GUARDIAN'
    END AS role,
    BIN_TO_UUID(
        CASE WHEN gl.senior_user_id = ? THEN gl.guardian_user_id
             ELSE gl.senior_user_id END
    , 1) AS counterpart_user_id,
    gl.relation,
    gl.permission,
    gl.status,
    gl.linked_at,
    gl.revoked_at,
    gl.revoked_by
FROM tbl_guardian_link gl
WHERE gl.senior_user_id = ? OR gl.guardian_user_id = ?
ORDER BY gl.created_at DESC;


-- ----------------------------------------------------------------------------
-- 8.6 사용자 탈퇴 시 데이터 현황 (법정 보관 기간 계산용)
-- ----------------------------------------------------------------------------
-- 탈퇴 후에도 전자금융거래법상 5년 보관 의무
SELECT
    u.user_id,
    u.status,
    u.withdrawn_at,
    DATE_ADD(u.withdrawn_at, INTERVAL 5 YEAR) AS retention_until,

    (SELECT COUNT(*) FROM tbl_transfer_order WHERE user_id = u.user_id) AS transfer_count,
    (SELECT COUNT(*) FROM tbl_consent_record WHERE user_id = u.user_id) AS consent_count,
    (SELECT COUNT(*) FROM tbl_monthly_report WHERE user_id = u.user_id) AS report_count,
    (SELECT COUNT(*) FROM tbl_linked_institution WHERE user_id = u.user_id) AS link_count
FROM tbl_user u
WHERE u.user_id = ?
  AND u.status = 'WITHDRAWN';


-- ----------------------------------------------------------------------------
-- 8.7 법정 보관 기간 만료 데이터 삭제 (배치, 월 1회)
-- ----------------------------------------------------------------------------
-- 탈퇴 + 5년 경과한 사용자 데이터 완전 삭제 (개인정보 파기 의무)
-- 단계적 삭제: 자식 테이블부터
START TRANSACTION;

-- 대상 user_id 선별 (임시 테이블 활용)
CREATE TEMPORARY TABLE tmp_expired_users (
    user_id BINARY(16) PRIMARY KEY
);

INSERT INTO tmp_expired_users (user_id)
SELECT user_id FROM tbl_user
WHERE status = 'WITHDRAWN'
  AND withdrawn_at < DATE_SUB(NOW(6), INTERVAL 5 YEAR)
LIMIT 1000;  -- 배치 처리

-- 순서 중요: FK depedency 따라 자식부터
DELETE FROM tbl_report_section
WHERE report_id IN (SELECT report_id FROM tbl_monthly_report
                    WHERE user_id IN (SELECT user_id FROM tmp_expired_users));

DELETE FROM tbl_monthly_report
WHERE user_id IN (SELECT user_id FROM tmp_expired_users);

DELETE FROM tbl_alert_subscription
WHERE link_id IN (SELECT link_id FROM tbl_guardian_link
                  WHERE senior_user_id IN (SELECT user_id FROM tmp_expired_users)
                     OR guardian_user_id IN (SELECT user_id FROM tmp_expired_users));

DELETE FROM tbl_guardian_link
WHERE senior_user_id IN (SELECT user_id FROM tmp_expired_users)
   OR guardian_user_id IN (SELECT user_id FROM tmp_expired_users);

DELETE FROM tbl_transfer_event
WHERE order_id IN (SELECT order_id FROM tbl_transfer_order
                   WHERE user_id IN (SELECT user_id FROM tmp_expired_users));

DELETE FROM tbl_transfer_order
WHERE user_id IN (SELECT user_id FROM tmp_expired_users);

DELETE FROM tbl_spending_limit
WHERE user_id IN (SELECT user_id FROM tmp_expired_users);

DELETE FROM tbl_daily_transfer_usage
WHERE user_id IN (SELECT user_id FROM tmp_expired_users);

DELETE FROM tbl_account_snapshot
WHERE account_id IN (SELECT account_id FROM tbl_account
                     WHERE link_id IN (SELECT link_id FROM tbl_linked_institution
                                       WHERE user_id IN (SELECT user_id FROM tmp_expired_users)));

DELETE FROM tbl_account
WHERE link_id IN (SELECT link_id FROM tbl_linked_institution
                  WHERE user_id IN (SELECT user_id FROM tmp_expired_users));

DELETE FROM tbl_linked_institution WHERE user_id IN (SELECT user_id FROM tmp_expired_users);
DELETE FROM tbl_biometric_credential WHERE user_id IN (SELECT user_id FROM tmp_expired_users);
DELETE FROM tbl_device WHERE user_id IN (SELECT user_id FROM tmp_expired_users);
DELETE FROM tbl_consent_record WHERE user_id IN (SELECT user_id FROM tmp_expired_users);
DELETE FROM tbl_user_profile WHERE user_id IN (SELECT user_id FROM tmp_expired_users);
DELETE FROM tbl_user WHERE user_id IN (SELECT user_id FROM tmp_expired_users);

DROP TEMPORARY TABLE tmp_expired_users;

COMMIT;
