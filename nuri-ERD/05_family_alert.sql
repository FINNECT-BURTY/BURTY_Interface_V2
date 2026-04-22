-- ============================================================================
-- USE CASE 05. 가족 공유 & 이상거래 알림 (보이스피싱 방어 핵심)
-- ----------------------------------------------------------------------------
-- 시나리오: 자녀가 부모 가입 유도 → 부모 동의 → 실시간 이체 알림
-- 빈도: 연결 수립은 드물지만, 알림 판별은 모든 이체마다
-- 핵심: 양쪽 동의 필수 + 알림 설정 세분화
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 5.1 자녀가 부모에게 연결 요청 (초대)
-- ----------------------------------------------------------------------------
-- 1단계: 자녀 측 동의만 먼저 기록 (부모 동의 대기 상태)
START TRANSACTION;

SET @guardian_consent_id = UUID_TO_BIN(UUID(), 1);
SET @link_id = UUID_TO_BIN(UUID(), 1);

INSERT INTO tbl_consent_record (
    consent_id, user_id, consent_type, consent_version,
    document_hash, agreed_at
) VALUES (
    @guardian_consent_id,
    ?,  -- guardian_user_id (자녀)
    'FAMILY_SHARE',
    'v1.0',
    ?,  -- 동의 문서 해시
    NOW(6)
);

-- 링크는 PENDING 상태로 생성 (부모 동의 대기)
-- senior_consent_id는 NULL NOT NULL 제약 때문에 일단 guardian 것과 동일하게 넣고
-- 부모 승인 시 갱신. 실제 설계에선 nullable로 바꾸는 것이 더 자연스러움.
-- 여기선 제약 준수 위해 임시 패턴:
INSERT INTO tbl_guardian_link (
    link_id, senior_user_id, guardian_user_id, relation,
    permission, senior_consent_id, guardian_consent_id,
    status
) VALUES (
    @link_id,
    ?,  -- senior_user_id (부모, 찾아낸 user_id)
    ?,  -- guardian_user_id (자녀)
    ?,  -- 'CHILD'
    'VIEW_ONLY',
    @guardian_consent_id,  -- 나중에 senior가 동의하면 갱신
    @guardian_consent_id,
    'PENDING'
);

COMMIT;


-- ----------------------------------------------------------------------------
-- 5.2 부모가 연결 승인
-- ----------------------------------------------------------------------------
-- 부모의 동의 기록 + 링크 ACTIVE 전환
START TRANSACTION;

SET @senior_consent_id = UUID_TO_BIN(UUID(), 1);

INSERT INTO tbl_consent_record (
    consent_id, user_id, consent_type, consent_version,
    document_hash, agreed_at
) VALUES (
    @senior_consent_id,
    ?,  -- senior_user_id (부모)
    'FAMILY_SHARE',
    'v1.0',
    ?, NOW(6)
);

UPDATE tbl_guardian_link
SET senior_consent_id = @senior_consent_id,
    status = 'ACTIVE',
    permission = ?,  -- 'VIEW_ONLY' | 'VIEW_AND_ALERT'
    linked_at = NOW(6),
    updated_at = NOW(6)
WHERE link_id = ?
  AND senior_user_id = ?  -- 본인 확인
  AND status = 'PENDING';

COMMIT;


-- ----------------------------------------------------------------------------
-- 5.3 부모의 연결된 자녀 목록 조회
-- ----------------------------------------------------------------------------
SELECT
    BIN_TO_UUID(gl.link_id, 1) AS link_id,
    BIN_TO_UUID(gl.guardian_user_id, 1) AS guardian_user_id,
    gl.relation,
    gl.permission,
    gl.status,
    gl.linked_at,
    COUNT(sub.subscription_id) AS alert_count
FROM tbl_guardian_link gl
LEFT JOIN tbl_alert_subscription sub
    ON sub.link_id = gl.link_id
   AND sub.is_active = TRUE
WHERE gl.senior_user_id = ?
  AND gl.status IN ('ACTIVE', 'PENDING')
GROUP BY gl.link_id;


-- ----------------------------------------------------------------------------
-- 5.4 자녀의 대시보드용 - 부모 자산 요약 (VIEW_ONLY 권한)
-- ----------------------------------------------------------------------------
-- 자녀가 볼 수 있는 부모 정보
SELECT
    BIN_TO_UUID(u.user_id, 1) AS senior_user_id,
    up.name_encrypted,  -- 자녀가 이미 아는 이름 (복호화 처리)
    COUNT(DISTINCT a.account_id) AS account_count,
    SUM(a.last_balance) AS total_balance,
    MAX(a.last_balance_at) AS last_synced_at
FROM tbl_guardian_link gl
INNER JOIN tbl_user u ON u.user_id = gl.senior_user_id
INNER JOIN tbl_user_profile up ON up.user_id = u.user_id
LEFT JOIN tbl_linked_institution li ON li.user_id = u.user_id AND li.status = 'ACTIVE'
LEFT JOIN tbl_account a ON a.link_id = li.link_id AND a.closed_at IS NULL
WHERE gl.guardian_user_id = ?  -- 자녀 user_id
  AND gl.status = 'ACTIVE'
GROUP BY u.user_id;


-- ----------------------------------------------------------------------------
-- 5.5 알림 구독 설정 등록 (자녀가 설정)
-- ----------------------------------------------------------------------------
-- 예: "부모님이 100만원 이상 이체하면 알려줘"
INSERT INTO tbl_alert_subscription (
    subscription_id, link_id, alert_type,
    threshold_amount, channel, is_active
) VALUES (
    UUID_TO_BIN(UUID(), 1),
    ?,  -- link_id
    'TRANSFER_OVER_AMOUNT',
    ?,  -- 1000000 (100만원)
    'PUSH',
    TRUE
)
ON DUPLICATE KEY UPDATE
    threshold_amount = VALUES(threshold_amount),
    is_active = TRUE,
    updated_at = NOW(6);


-- ----------------------------------------------------------------------------
-- 5.6 이체 발생 시 알림 대상자 조회 (핵심 방어 로직)
-- ----------------------------------------------------------------------------
-- 이체가 실행되는 순간, 알림 받을 자녀를 모두 찾아냄
-- 조건: 부모의 이체 금액 >= threshold이거나 심야 시간대
SELECT DISTINCT
    BIN_TO_UUID(gl.guardian_user_id, 1) AS guardian_user_id,
    sub.alert_type,
    sub.channel,
    d.fcm_token
FROM tbl_guardian_link gl
INNER JOIN tbl_alert_subscription sub
    ON sub.link_id = gl.link_id
   AND sub.is_active = TRUE
INNER JOIN tbl_device d
    ON d.user_id = gl.guardian_user_id
   AND d.revoked_at IS NULL
   AND d.fcm_token IS NOT NULL
WHERE gl.senior_user_id = ?  -- 이체한 부모
  AND gl.status = 'ACTIVE'
  AND gl.permission = 'VIEW_AND_ALERT'
  AND (
      (sub.alert_type = 'TRANSFER_OVER_AMOUNT' AND sub.threshold_amount <= ?)
      OR
      (sub.alert_type = 'LATE_NIGHT_TRANSACTION'
       AND TIME(NOW()) BETWEEN COALESCE(sub.threshold_time_from, '22:00:00')
                          AND COALESCE(sub.threshold_time_to, '06:00:00'))
  );


-- ----------------------------------------------------------------------------
-- 5.7 알림 발송 기록 (tbl_notification)
-- ----------------------------------------------------------------------------
INSERT INTO tbl_notification (
    recipient_user_id, notification_type, channel,
    title, body, deep_link,
    related_entity_type, related_entity_id, status, created_at
) VALUES (
    ?,  -- guardian_user_id
    'TRANSFER_ALERT',
    'PUSH',
    '어머님이 1,000만원을 이체하셨어요',
    '카카오뱅크 적금으로 방금 이체가 완료됐어요.',
    'nuri://family/transfer/{order_id}',
    'transfer_order',
    ?,  -- order_id
    'QUEUED',
    NOW(6)
);


-- ----------------------------------------------------------------------------
-- 5.8 미읽은 알림 개수 (앱 아이콘 뱃지용)
-- ----------------------------------------------------------------------------
-- 성능 목표: < 5ms (idx_notification_user_created)
SELECT COUNT(*) AS unread_count
FROM tbl_notification
WHERE recipient_user_id = ?
  AND status IN ('SENT', 'DELIVERED')
  AND read_at IS NULL
  AND created_at >= DATE_SUB(NOW(6), INTERVAL 30 DAY);


-- ----------------------------------------------------------------------------
-- 5.9 알림 읽음 처리
-- ----------------------------------------------------------------------------
UPDATE tbl_notification
SET status = 'READ',
    read_at = NOW(6)
WHERE notification_id = ?
  AND recipient_user_id = ?  -- 본인 확인
  AND read_at IS NULL;


-- ----------------------------------------------------------------------------
-- 5.10 가족 연결 해제 (부모 또는 자녀 어느 쪽이든)
-- ----------------------------------------------------------------------------
-- soft delete + 알림 구독 비활성화
START TRANSACTION;

UPDATE tbl_guardian_link
SET status = 'REVOKED',
    revoked_at = NOW(6),
    revoked_by = ?,  -- 'SENIOR' | 'GUARDIAN'
    updated_at = NOW(6)
WHERE link_id = ?
  AND status = 'ACTIVE';

UPDATE tbl_alert_subscription
SET is_active = FALSE,
    updated_at = NOW(6)
WHERE link_id = ?;

-- 동의도 revoke
UPDATE tbl_consent_record
SET revoked_at = NOW(6),
    revoke_reason = 'Guardian link revoked'
WHERE consent_id IN (
    SELECT senior_consent_id FROM tbl_guardian_link WHERE link_id = ?
    UNION
    SELECT guardian_consent_id FROM tbl_guardian_link WHERE link_id = ?
)
  AND consent_type = 'FAMILY_SHARE'
  AND revoked_at IS NULL;

COMMIT;


-- ----------------------------------------------------------------------------
-- 5.11 이상거래 패턴 탐지 (심야 + 고액 + 신규 계좌)
-- ----------------------------------------------------------------------------
-- 보이스피싱 의심 거래 탐지 쿼리 (실시간 분석용)
-- 조건 3개 중 2개 이상 해당되면 경고
SELECT
    BIN_TO_UUID(o.order_id, 1) AS order_id,
    o.amount,
    o.to_account_no_masked,
    o.created_at,
    TIME(o.created_at) AS tx_time,

    -- 이상 지표
    (TIME(o.created_at) BETWEEN '22:00:00' AND '06:00:00') AS is_late_night,
    (o.amount >= 5000000) AS is_large_amount,
    (NOT EXISTS (
        SELECT 1 FROM tbl_transfer_order o2
        WHERE o2.user_id = o.user_id
          AND o2.to_account_no_encrypted = o.to_account_no_encrypted
          AND o2.order_id <> o.order_id
          AND o2.status = 'EXECUTED'
          AND o2.created_at < DATE_SUB(o.created_at, INTERVAL 1 DAY)
    )) AS is_new_recipient,

    -- 의심 점수 (0-3)
    ((TIME(o.created_at) BETWEEN '22:00:00' AND '06:00:00')
     + (o.amount >= 5000000)
     + (NOT EXISTS (
         SELECT 1 FROM tbl_transfer_order o2
         WHERE o2.user_id = o.user_id
           AND o2.to_account_no_encrypted = o.to_account_no_encrypted
           AND o2.order_id <> o.order_id
           AND o2.status = 'EXECUTED'
           AND o2.created_at < DATE_SUB(o.created_at, INTERVAL 1 DAY)
       ))
    ) AS suspicion_score
FROM tbl_transfer_order o
WHERE o.user_id = ?
  AND o.order_id = ?;
