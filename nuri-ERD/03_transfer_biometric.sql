-- ============================================================================
-- USE CASE 03. 이체 실행 & 3단계 권한 (NURI 핵심)
-- ----------------------------------------------------------------------------
-- 시나리오: AI가 이체 제안 → 사용자 지문 인증 → 은행 API 호출 → Audit
-- 빈도: 일 수백 건 (금액 따라 다름)
-- 핵심: Event Sourcing으로 모든 상태 변화 추적
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 3.1 [STEP 1] 이체 주문 생성 (AI 에이전트가 호출)
-- ----------------------------------------------------------------------------
-- 사용자: "1000만원 카카오뱅크 적금으로 옮겨줘"
-- AI 에이전트가 Tool Call로 이 쿼리 실행
START TRANSACTION;

SET @order_id = UUID_TO_BIN(UUID(), 1);
SET @idem_key = SHA2(CONCAT_WS('|', ?, ?, ?, ?, UNIX_TIMESTAMP(NOW(6))), 256);

-- 한도 체크 먼저 (일일 한도)
SELECT
    (COALESCE(u.total_amount, 0) + ?) > l.amount_limit AS exceeds_daily_limit
FROM tbl_spending_limit l
LEFT JOIN tbl_daily_transfer_usage u
    ON u.user_id = l.user_id
   AND u.usage_date = CURDATE()
WHERE l.user_id = ?
  AND l.period_type = 'DAILY'
  AND l.effective_from <= NOW(6)
  AND (l.effective_to IS NULL OR l.effective_to > NOW(6))
ORDER BY l.effective_from DESC
LIMIT 1;

-- 한도 통과 시 주문 생성
INSERT INTO tbl_transfer_order (
    order_id, user_id, idempotency_key,
    from_account_id, to_account_no_encrypted, to_account_no_masked,
    to_bank_code, to_holder_name, amount, memo, purpose, status
) VALUES (
    @order_id,
    ?,              -- user_id
    @idem_key,
    ?,              -- from_account_id
    ?, ?, ?, ?,     -- 수취 계좌 정보
    ?,              -- 금액
    ?, 'TRANSFER', 'PENDING'
);

-- 이벤트 기록 (seq=1)
INSERT INTO tbl_transfer_event (
    order_id, sequence_no, event_type, payload, actor_type, actor_id, occurred_at
) VALUES (
    @order_id, 1, 'CREATED',
    JSON_OBJECT('source', 'ai_agent', 'conversation_id', ?),
    'AI_AGENT', ?, NOW(6)
);

COMMIT;


-- ----------------------------------------------------------------------------
-- 3.2 [STEP 2] 인증 요청 (FIDO2 challenge 발급)
-- ----------------------------------------------------------------------------
-- Redis에 challenge 저장 후 이벤트 기록
UPDATE tbl_transfer_order
SET status = 'AUTH_REQUESTED',
    updated_at = NOW(6)
WHERE order_id = ?
  AND status = 'PENDING';

INSERT INTO tbl_transfer_event (
    order_id, sequence_no, event_type, payload, actor_type, occurred_at
) VALUES (
    ?,  -- order_id
    2,  -- seq
    'AUTH_REQUESTED',
    JSON_OBJECT('challenge_id', ?, 'credential_types', JSON_ARRAY('FINGERPRINT', 'FACE_ID')),
    'SYSTEM',
    NOW(6)
);


-- ----------------------------------------------------------------------------
-- 3.3 [STEP 3] 생체 인증 검증 통과 (사용자 지문 찍음)
-- ----------------------------------------------------------------------------
-- 사용 가능한 biometric credential 조회 (인증 시점)
SELECT
    BIN_TO_UUID(credential_id, 1) AS credential_id,
    public_key,
    sign_count
FROM tbl_biometric_credential
WHERE user_id = ?
  AND device_id = ?
  AND revoked_at IS NULL
LIMIT 1;

-- WebAuthn 검증 통과 후 sign_count 증가 + 주문에 연결
START TRANSACTION;

UPDATE tbl_biometric_credential
SET sign_count = sign_count + 1,
    last_used_at = NOW(6)
WHERE credential_id = ?
  AND sign_count = ?;  -- 낙관적 락 (전달된 sign_count와 일치해야 함 - 재전송 방지)

UPDATE tbl_transfer_order
SET status = 'AUTHORIZED',
    biometric_credential_id = ?,
    updated_at = NOW(6)
WHERE order_id = ?
  AND status = 'AUTH_REQUESTED';

INSERT INTO tbl_transfer_event (
    order_id, sequence_no, event_type, payload, actor_type, actor_id, occurred_at
) VALUES (
    ?, 3, 'AUTH_APPROVED',
    JSON_OBJECT(
        'credential_id', ?,
        'credential_type', ?,
        'device_id', ?
    ),
    'USER', ?, NOW(6)
);

COMMIT;


-- ----------------------------------------------------------------------------
-- 3.4 [STEP 4] 은행 API 호출 직전 상태 기록
-- ----------------------------------------------------------------------------
UPDATE tbl_transfer_order
SET status = 'EXECUTING',
    updated_at = NOW(6)
WHERE order_id = ?
  AND status = 'AUTHORIZED';

INSERT INTO tbl_transfer_event (
    order_id, sequence_no, event_type, payload, actor_type, occurred_at
) VALUES (
    ?, 4, 'BANK_CALLED',
    JSON_OBJECT('bank_code', ?, 'request_id', ?),
    'SYSTEM', NOW(6)
);


-- ----------------------------------------------------------------------------
-- 3.5 [STEP 5] 은행 응답 처리 - 성공 케이스
-- ----------------------------------------------------------------------------
START TRANSACTION;

UPDATE tbl_transfer_order
SET status = 'EXECUTED',
    bank_transaction_id = ?,
    executed_at = NOW(6),
    updated_at = NOW(6)
WHERE order_id = ?
  AND status = 'EXECUTING';

INSERT INTO tbl_transfer_event (
    order_id, sequence_no, event_type, payload, actor_type, occurred_at
) VALUES (
    ?, 5, 'BANK_RESPONDED',
    JSON_OBJECT('bank_tx_id', ?, 'response_code', '0000'),
    'BANK', NOW(6)
),
(
    ?, 6, 'EXECUTED',
    JSON_OBJECT('final_status', 'success'),
    'SYSTEM', NOW(6)
);

-- 일일 사용량 갱신 (한도 체크용)
INSERT INTO tbl_daily_transfer_usage (
    user_id, usage_date, total_amount, transfer_count
) VALUES (
    ?, CURDATE(), ?, 1
)
ON DUPLICATE KEY UPDATE
    total_amount = total_amount + VALUES(total_amount),
    transfer_count = transfer_count + 1,
    updated_at = NOW(6);

COMMIT;


-- ----------------------------------------------------------------------------
-- 3.6 [STEP 5-FAIL] 은행 응답 처리 - 실패 케이스
-- ----------------------------------------------------------------------------
START TRANSACTION;

UPDATE tbl_transfer_order
SET status = 'FAILED',
    failed_reason = ?,
    updated_at = NOW(6)
WHERE order_id = ?
  AND status = 'EXECUTING';

INSERT INTO tbl_transfer_event (
    order_id, sequence_no, event_type, payload, actor_type, occurred_at
) VALUES (
    ?, 5, 'FAILED',
    JSON_OBJECT('error_code', ?, 'error_message', ?, 'retry_possible', ?),
    'BANK', NOW(6)
);

COMMIT;


-- ----------------------------------------------------------------------------
-- 3.7 이체 주문 전체 이력 조회 (감사 대응)
-- ----------------------------------------------------------------------------
-- "이 이체가 어떻게 진행됐는지 전부 보여줘"
SELECT
    o.status AS current_status,
    o.amount,
    o.to_account_no_masked,
    e.sequence_no,
    e.event_type,
    e.actor_type,
    e.occurred_at,
    e.payload,
    CASE
        WHEN e.event_type = 'AUTH_APPROVED' THEN
            (SELECT credential_type FROM tbl_biometric_credential
             WHERE credential_id = o.biometric_credential_id)
        ELSE NULL
    END AS biometric_type
FROM tbl_transfer_order o
INNER JOIN tbl_transfer_event e ON e.order_id = o.order_id
WHERE o.order_id = ?
ORDER BY e.sequence_no;


-- ----------------------------------------------------------------------------
-- 3.8 사용자별 최근 이체 내역 (앱 홈 - 최근 활동)
-- ----------------------------------------------------------------------------
-- 성능 목표: < 30ms (idx_transfer_user_created 커버)
SELECT
    BIN_TO_UUID(o.order_id, 1) AS order_id,
    o.amount,
    o.to_account_no_masked,
    o.to_holder_name,
    o.status,
    o.executed_at,
    o.created_at,
    li.institution_name AS from_institution,
    a.account_no_masked AS from_account_masked
FROM tbl_transfer_order o
INNER JOIN tbl_account a ON a.account_id = o.from_account_id
INNER JOIN tbl_linked_institution li ON li.link_id = a.link_id
WHERE o.user_id = ?
ORDER BY o.created_at DESC
LIMIT 20;


-- ----------------------------------------------------------------------------
-- 3.9 현재 유효한 한도 조회
-- ----------------------------------------------------------------------------
-- Temporal 패턴: 현재 시점에 유효한 한도 설정
SELECT
    period_type,
    amount_limit,
    effective_from,
    changed_by
FROM tbl_spending_limit
WHERE user_id = ?
  AND effective_from <= NOW(6)
  AND (effective_to IS NULL OR effective_to > NOW(6))
ORDER BY period_type, effective_from DESC;


-- ----------------------------------------------------------------------------
-- 3.10 한도 변경 (이력 보존 방식)
-- ----------------------------------------------------------------------------
-- UPDATE 아닌 INSERT → 기존 한도는 effective_to로 종료
START TRANSACTION;

-- 기존 활성 한도 종료
UPDATE tbl_spending_limit
SET effective_to = NOW(6)
WHERE user_id = ?
  AND period_type = ?
  AND effective_to IS NULL;

-- 새 한도 삽입
INSERT INTO tbl_spending_limit (
    limit_id, user_id, period_type, amount_limit,
    effective_from, changed_by, change_reason
) VALUES (
    UUID_TO_BIN(UUID(), 1),
    ?, ?, ?,
    NOW(6),
    ?,  -- 'USER' | 'GUARDIAN' | 'SYSTEM' | 'COMPLIANCE'
    ?
);

COMMIT;
