-- ============================================================================
-- NURI (Team Nest) - 유스케이스별 쿼리 가이드
-- ============================================================================
-- 대상 DB: MariaDB 10.11+ / MySQL 8.0+
-- 문자셋:  utf8mb4 / utf8mb4_0900_ai_ci
-- 엔진:    InnoDB
--
-- 구성:
--   UC-01. 회원가입 & 본인인증
--   UC-02. 마이데이터 자산 연동 & 자산 조회
--   UC-03. 이체 실행 & 3단계 권한 (NURI 핵심)
--   UC-04. Easy-Read 월간 리포트 생성 & 조회
--   UC-05. 가족 공유 & 이상거래 알림 (보이스피싱 방어)
--   UC-06. 운영 모니터링 & 대시보드
--   UC-07. AI 에이전트 대화 (MongoDB 참조)
--   UC-08. 감사·규제 대응 (개인정보법·전자금융거래법)
--
-- 쿼리 작성 원칙:
--   - 모든 테이블 tbl_ 접두사
--   - UUID는 BINARY(16), UUID_TO_BIN(UUID(), 1) 사용
--   - 금액은 BIGINT 원 단위
--   - DATETIME(6) 마이크로초 정밀도
--   - 하드 삭제 금지, soft delete + 법정 보관 기간 유지
--   - 감사 대상 테이블은 append-only (revoked_at으로 논리 종료)
-- ============================================================================

-- ============================================================================
-- USE CASE 01. 회원가입 & 본인인증
-- ----------------------------------------------------------------------------
-- 시나리오: 사용자가 앱을 처음 설치하고 휴대폰 본인인증을 거쳐 계정을 생성
-- 빈도: 일 100~1000건 (초기), 일 1만건 (정식 런칭 후)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1.1 기존 가입자 확인 (회원가입 전)
-- ----------------------------------------------------------------------------
-- CI 해시로 이미 가입된 사용자인지 확인
-- 성능 목표: < 10ms (uk_user_ci_hash 활용)
SELECT
    BIN_TO_UUID(user_id, 1)  AS user_id,
    status,
    created_at
FROM tbl_user
WHERE ci_hash = ?  -- SHA-256(CI)
  AND status <> 'WITHDRAWN';


-- ----------------------------------------------------------------------------
-- 1.2 신규 사용자 가입 (트랜잭션)
-- ----------------------------------------------------------------------------
-- tbl_user, tbl_user_profile, tbl_consent_record 동시 생성
START TRANSACTION;

SET @user_id = UUID_TO_BIN(UUID(), 1);

INSERT INTO tbl_user (
    user_id, ci_hash, ci_encrypted,
    phone_hash, phone_encrypted, status
) VALUES (
    @user_id,
    ?,  -- SHA-256(CI)
    ?,  -- AES-256-GCM(CI)
    ?,  -- SHA-256(phone)
    ?,  -- AES-256-GCM(phone)
    'ACTIVE'
);

INSERT INTO tbl_user_profile (
    user_id, name_encrypted, birthdate_encrypted,
    age_range, ux_mode, font_scale, voice_enabled
) VALUES (
    @user_id,
    ?,  -- AES-256-GCM(name)
    ?,  -- AES-256-GCM(YYYYMMDD)
    ?,  -- 연령대 계산 결과 (55s=55, 60s=60, 65s=65)
    CASE WHEN ? >= 55 THEN 'SENIOR' ELSE 'STANDARD' END,  -- 55세 이상 시니어 모드
    CASE WHEN ? >= 55 THEN 1.30 ELSE 1.00 END,
    CASE WHEN ? >= 55 THEN TRUE ELSE FALSE END
);

-- 필수 동의 3개를 한 번에 기록
INSERT INTO tbl_consent_record (
    consent_id, user_id, consent_type, consent_version,
    document_hash, agreed_at, ip_address, user_agent
) VALUES
    (UUID_TO_BIN(UUID(), 1), @user_id, 'TERMS',   'v2.1', ?, NOW(6), ?, ?),
    (UUID_TO_BIN(UUID(), 1), @user_id, 'PRIVACY', 'v2.1', ?, NOW(6), ?, ?),
    (UUID_TO_BIN(UUID(), 1), @user_id, 'MYDATA',  'v1.3', ?, NOW(6), ?, ?);

COMMIT;


-- ----------------------------------------------------------------------------
-- 1.3 디바이스 등록 (로그인 시)
-- ----------------------------------------------------------------------------
-- 같은 디바이스 재로그인 시 UPSERT
INSERT INTO tbl_device (
    device_id, user_id, device_fingerprint, platform,
    os_version, app_version, fcm_token, last_seen_at
) VALUES (
    UUID_TO_BIN(UUID(), 1),
    ?,  -- user_id
    ?,  -- device fingerprint (SHA-256)
    ?,  -- 'IOS' | 'ANDROID' | 'WEB'
    ?, ?, ?,
    NOW(6)
)
ON DUPLICATE KEY UPDATE
    os_version = VALUES(os_version),
    app_version = VALUES(app_version),
    fcm_token = VALUES(fcm_token),
    last_seen_at = NOW(6),
    revoked_at = NULL;  -- 재활성화


-- ----------------------------------------------------------------------------
-- 1.4 FIDO2 생체인증 등록
-- ----------------------------------------------------------------------------
-- WebAuthn 공개키 등록 (지문/Face ID)
INSERT INTO tbl_biometric_credential (
    credential_id, user_id, device_id, credential_type,
    public_key, credential_id_raw, aaguid, registered_at
) VALUES (
    UUID_TO_BIN(UUID(), 1),
    ?,  -- user_id
    ?,  -- device_id
    ?,  -- 'FINGERPRINT' | 'FACE_ID'
    ?,  -- 공개키 바이너리
    ?,  -- WebAuthn credential ID
    ?,  -- 인증기 AAGUID
    NOW(6)
);


-- ----------------------------------------------------------------------------
-- 1.5 로그인 (디바이스 검증 + 실패 카운트)
-- ----------------------------------------------------------------------------
-- 로그인 실패 시 카운트 증가 (5회 초과 시 앱 레벨에서 차단)
UPDATE tbl_user
SET failed_login_count = failed_login_count + 1,
    updated_at = NOW(6)
WHERE user_id = ?;

-- 로그인 성공 시 카운트 초기화 + 최종 로그인 갱신
UPDATE tbl_user
SET failed_login_count = 0,
    last_login_at = NOW(6),
    last_login_ip = INET6_ATON(?),  -- IP 문자열 -> binary
    updated_at = NOW(6)
WHERE user_id = ?;


-- ----------------------------------------------------------------------------
-- 1.6 유효한 동의 조회 (마이데이터 연동 전 체크)
-- ----------------------------------------------------------------------------
-- 성능 목표: < 5ms (idx_consent_user_type 커버)
SELECT
    consent_type,
    consent_version,
    agreed_at
FROM tbl_consent_record
WHERE user_id = ?
  AND consent_type IN ('TERMS', 'PRIVACY', 'MYDATA')
  AND revoked_at IS NULL
ORDER BY agreed_at DESC;


-- ----------------------------------------------------------------------------
-- 1.7 동의 철회 (append-only, UPDATE로 revoked_at 기록)
-- ----------------------------------------------------------------------------
UPDATE tbl_consent_record
SET revoked_at = NOW(6),
    revoke_reason = ?
WHERE consent_id = ?
  AND revoked_at IS NULL;


-- ----------------------------------------------------------------------------
-- 1.8 회원 탈퇴 (soft delete, 금융 데이터는 법정 보관 기간 유지)
-- ----------------------------------------------------------------------------
-- 하드 삭제 절대 금지. status만 변경.
START TRANSACTION;

UPDATE tbl_user
SET status = 'WITHDRAWN',
    withdrawn_at = NOW(6),
    updated_at = NOW(6)
WHERE user_id = ?;

-- 모든 디바이스 revoke
UPDATE tbl_device
SET revoked_at = NOW(6)
WHERE user_id = ?
  AND revoked_at IS NULL;

-- 모든 생체 자격증명 revoke
UPDATE tbl_biometric_credential
SET revoked_at = NOW(6)
WHERE user_id = ?
  AND revoked_at IS NULL;

-- 마이데이터 연결 전부 REVOKED
UPDATE tbl_linked_institution
SET status = 'REVOKED',
    updated_at = NOW(6)
WHERE user_id = ?
  AND status = 'ACTIVE';

COMMIT;
-- ============================================================================
-- USE CASE 02. 마이데이터 자산 연동 & 자산 조회
-- ----------------------------------------------------------------------------
-- 시나리오 1: 사용자가 마이데이터로 은행/카드/증권 연동
-- 시나리오 2: 홈 화면에서 전체 자산 조회
-- 빈도: 자산 조회는 초당 수천 건 (앱 홈 진입 시마다)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 2.1 마이데이터 기관 연동 등록
-- ----------------------------------------------------------------------------
-- access_token은 KMS로 암호화 후 저장
INSERT INTO tbl_linked_institution (
    link_id, user_id, institution_code, institution_name, institution_type,
    access_token_encrypted, refresh_token_encrypted,
    token_expires_at, consent_expires_at, status, last_synced_at
) VALUES (
    UUID_TO_BIN(UUID(), 1),
    ?,   -- user_id
    ?,   -- 'BANK_001' (카카오뱅크 예시)
    ?,   -- '카카오뱅크'
    ?,   -- 'BANK' | 'CARD' | 'SECURITIES' | 'PENSION'
    ?,   -- KMS::encrypt(access_token)
    ?,   -- KMS::encrypt(refresh_token)
    DATE_ADD(NOW(6), INTERVAL ? SECOND),   -- expires_in
    DATE_ADD(NOW(6), INTERVAL 1 YEAR),      -- 마이데이터 동의 1년
    'ACTIVE',
    NOW(6)
);


-- ----------------------------------------------------------------------------
-- 2.2 연동된 기관별 access_token 만료 임박 체크 (배치, 매일)
-- ----------------------------------------------------------------------------
-- 24시간 내 만료되는 토큰 조회 → 리프레시 배치 트리거
-- 성능 목표: < 100ms (idx_link_status_expires 활용)
SELECT
    BIN_TO_UUID(link_id, 1) AS link_id,
    BIN_TO_UUID(user_id, 1) AS user_id,
    institution_code,
    institution_name,
    token_expires_at,
    consent_expires_at
FROM tbl_linked_institution
WHERE status = 'ACTIVE'
  AND token_expires_at < DATE_ADD(NOW(6), INTERVAL 24 HOUR)
ORDER BY token_expires_at
LIMIT 1000;


-- ----------------------------------------------------------------------------
-- 2.3 마이데이터 동의 만료 30일 전 알림 대상 조회
-- ----------------------------------------------------------------------------
-- 사용자에게 미리 알려 재동의 유도
SELECT
    BIN_TO_UUID(li.user_id, 1) AS user_id,
    GROUP_CONCAT(li.institution_name SEPARATOR ', ') AS institutions,
    MIN(li.consent_expires_at) AS nearest_expiry
FROM tbl_linked_institution li
WHERE li.status = 'ACTIVE'
  AND li.consent_expires_at BETWEEN NOW(6) AND DATE_ADD(NOW(6), INTERVAL 30 DAY)
GROUP BY li.user_id
HAVING nearest_expiry IS NOT NULL;


-- ----------------------------------------------------------------------------
-- 2.4 계좌 신규 등록 (동기화 시 계좌 추가)
-- ----------------------------------------------------------------------------
-- 이미 존재하는 계좌는 무시 (account_no_hash UNIQUE로 차단)
INSERT IGNORE INTO tbl_account (
    account_id, link_id,
    account_no_encrypted, account_no_hash, account_no_masked,
    account_name, account_type, currency, is_primary
) VALUES (
    UUID_TO_BIN(UUID(), 1),
    ?,   -- link_id
    ?,   -- AES-256-GCM(account_no)
    ?,   -- SHA-256(account_no) + salt
    ?,   -- '***1234'
    ?, ?, 'KRW', FALSE
);


-- ----------------------------------------------------------------------------
-- 2.5 홈 화면 자산 요약 (핵심 쿼리, 캐싱 대상)
-- ----------------------------------------------------------------------------
-- 사용자의 전체 자산을 한 번에 조회
-- 성능 목표: < 50ms (Redis 캐시 우선, DB fallback)
-- 캐시 키: home:summary:{user_id}, TTL 5분
SELECT
    a.account_type,
    COUNT(*) AS account_count,
    SUM(a.last_balance) AS total_balance,
    GROUP_CONCAT(
        JSON_OBJECT(
            'account_id', BIN_TO_UUID(a.account_id, 1),
            'institution_name', li.institution_name,
            'account_no_masked', a.account_no_masked,
            'account_name', a.account_name,
            'balance', a.last_balance,
            'last_balance_at', a.last_balance_at
        )
        ORDER BY a.last_balance DESC
    ) AS accounts_json
FROM tbl_account a
INNER JOIN tbl_linked_institution li ON li.link_id = a.link_id
WHERE li.user_id = ?
  AND li.status = 'ACTIVE'
  AND a.closed_at IS NULL
GROUP BY a.account_type
ORDER BY FIELD(a.account_type,
    'DEPOSIT', 'SAVINGS', 'CHECKING',
    'STOCK', 'FUND', 'PENSION', 'LOAN', 'CARD');


-- ----------------------------------------------------------------------------
-- 2.6 특정 계좌의 잔액 갱신 (동기화 시)
-- ----------------------------------------------------------------------------
UPDATE tbl_account
SET last_balance = ?,
    last_balance_at = NOW(6),
    updated_at = NOW(6)
WHERE account_id = ?;


-- ----------------------------------------------------------------------------
-- 2.7 일별 자산 스냅샷 기록 (매일 자정 배치)
-- ----------------------------------------------------------------------------
-- 모든 ACTIVE 계좌의 당일 스냅샷 저장 (과거 시점 조회용)
INSERT INTO tbl_account_snapshot (
    account_id, as_of_date, balance, available_balance, holdings
)
SELECT
    a.account_id,
    CURDATE(),
    a.last_balance,
    a.last_balance,  -- 실제로는 마이데이터에서 available_balance 별도 수신
    NULL
FROM tbl_account a
INNER JOIN tbl_linked_institution li ON li.link_id = a.link_id
WHERE li.status = 'ACTIVE'
  AND a.closed_at IS NULL
  AND a.last_balance IS NOT NULL
ON DUPLICATE KEY UPDATE
    balance = VALUES(balance),
    captured_at = NOW(6);


-- ----------------------------------------------------------------------------
-- 2.8 자산 추이 조회 (월간 리포트용)
-- ----------------------------------------------------------------------------
-- 최근 13개월 (전년 동월 비교 가능) 월말 자산
-- 성능 목표: < 200ms (파티션 프루닝)
SELECT
    DATE_FORMAT(as_of_date, '%Y-%m') AS month,
    SUM(balance) AS total_balance
FROM tbl_account_snapshot s
INNER JOIN tbl_account a ON a.account_id = s.account_id
INNER JOIN tbl_linked_institution li ON li.link_id = a.link_id
WHERE li.user_id = ?
  AND s.as_of_date >= DATE_SUB(CURDATE(), INTERVAL 13 MONTH)
  AND s.as_of_date = LAST_DAY(s.as_of_date)  -- 월말만
GROUP BY month
ORDER BY month;


-- ----------------------------------------------------------------------------
-- 2.9 "지난달 대비 자산 변화" (AI 분석용 핵심 쿼리)
-- ----------------------------------------------------------------------------
-- Easy-Read: "이번 달 자산은 지난달보다 2% 늘었어요"
SELECT
    (SELECT SUM(balance) FROM tbl_account_snapshot s1
     INNER JOIN tbl_account a1 ON a1.account_id = s1.account_id
     INNER JOIN tbl_linked_institution li1 ON li1.link_id = a1.link_id
     WHERE li1.user_id = ?
       AND s1.as_of_date = LAST_DAY(DATE_SUB(CURDATE(), INTERVAL 1 MONTH))
    ) AS prev_month_balance,
    (SELECT SUM(balance) FROM tbl_account_snapshot s2
     INNER JOIN tbl_account a2 ON a2.account_id = s2.account_id
     INNER JOIN tbl_linked_institution li2 ON li2.link_id = a2.link_id
     WHERE li2.user_id = ?
       AND s2.as_of_date = LAST_DAY(CURDATE())
    ) AS this_month_balance;
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
-- ============================================================================
-- USE CASE 04. Easy-Read 월간 리포트 생성 & 조회
-- ----------------------------------------------------------------------------
-- 시나리오: 매달 1일 오전 9시, 모든 활성 사용자에게 리포트 생성 배치
-- 빈도: 월 1회 배치 (사용자 수만큼)
-- 핵심: 신호등 색상 + 3문장 요약 + 1액션 버튼
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 4.1 리포트 생성 대상자 조회 (배치 시작)
-- ----------------------------------------------------------------------------
-- 이번 달 리포트가 아직 없는 활성 사용자
SELECT
    BIN_TO_UUID(u.user_id, 1) AS user_id,
    up.ux_mode,
    up.font_scale,
    up.voice_enabled
FROM tbl_user u
INNER JOIN tbl_user_profile up ON up.user_id = u.user_id
WHERE u.status = 'ACTIVE'
  AND NOT EXISTS (
      SELECT 1 FROM tbl_monthly_report r
      WHERE r.user_id = u.user_id
        AND r.period_month = DATE_FORMAT(CURDATE(), '%Y-%m-01')
  )
LIMIT 10000;  -- 배치당 최대 처리량


-- ----------------------------------------------------------------------------
-- 4.2 리포트 생성 시작 (GENERATING 상태로 선점)
-- ----------------------------------------------------------------------------
INSERT INTO tbl_monthly_report (
    report_id, user_id, period_month, status, created_at
) VALUES (
    UUID_TO_BIN(UUID(), 1),
    ?,
    DATE_FORMAT(CURDATE(), '%Y-%m-01'),
    'GENERATING',
    NOW(6)
)
ON DUPLICATE KEY UPDATE
    status = 'GENERATING',
    failed_reason = NULL,
    updated_at = NOW(6);


-- ----------------------------------------------------------------------------
-- 4.3 리포트용 데이터 수집 (지난 달 지출 카테고리별)
-- ----------------------------------------------------------------------------
-- raw_transaction은 MongoDB에 있으므로 여기선 집계된 결과를 받아오는 가정
-- MariaDB에서 가능한 부분: 자산 추이, 이체 빈도
SELECT
    COUNT(DISTINCT o.order_id) AS transfer_count,
    COALESCE(SUM(o.amount), 0) AS total_transferred,
    AVG(o.amount) AS avg_transfer_amount,
    COUNT(DISTINCT CASE WHEN o.status = 'FAILED' THEN o.order_id END) AS failed_count
FROM tbl_transfer_order o
WHERE o.user_id = ?
  AND o.created_at >= DATE_SUB(CURDATE(), INTERVAL 1 MONTH)
  AND o.created_at < CURDATE();


-- ----------------------------------------------------------------------------
-- 4.4 리포트 섹션 저장 (AI가 생성한 결과)
-- ----------------------------------------------------------------------------
-- AI 응답: raw_data + easy_read_text + 신호등 색상 계산 결과
INSERT INTO tbl_report_section (
    section_id, report_id, section_type, display_order,
    easy_read_text, raw_data, traffic_light,
    action_label, action_payload
) VALUES
-- 1. 요약 섹션
(
    UUID_TO_BIN(UUID(), 1),
    ?,  -- report_id
    'SUMMARY',
    1,
    '이번 달 자산이 지난달보다 2% 늘었어요. 큰 변화 없이 잘 굴러갔어요.',
    JSON_OBJECT('prev_balance', 420000000, 'current_balance', 428400000, 'change_pct', 2.0),
    'GREEN',
    NULL, NULL
),
-- 2. 지출 섹션
(
    UUID_TO_BIN(UUID(), 1),
    ?,  -- report_id
    'SPENDING',
    2,
    '이번 달 병원비가 평소보다 조금 많이 나갔어요. 하지만 아직 괜찮은 수준이에요.',
    JSON_OBJECT(
        'total_spending', 2847000,
        'prev_month', 2410000,
        'change_pct', 18.1,
        'top_category', '의료',
        'categories', JSON_ARRAY(
            JSON_OBJECT('name', '의료', 'amount', 680000),
            JSON_OBJECT('name', '식비', 'amount', 520000)
        )
    ),
    'YELLOW',
    '상세 보기',
    JSON_OBJECT('screen', 'spending_detail', 'month', '2026-03')
),
-- 3. 액션 섹션
(
    UUID_TO_BIN(UUID(), 1),
    ?,  -- report_id
    'ACTION',
    3,
    '다음 달 외식비 60만원 한도 설정해볼까요?',
    JSON_OBJECT('suggested_limit', 600000, 'category', '외식'),
    'GREEN',
    '한도 설정',
    JSON_OBJECT('screen', 'spending_limit', 'preset_amount', 600000)
);


-- ----------------------------------------------------------------------------
-- 4.5 리포트 완료 처리 (PDF 생성 후)
-- ----------------------------------------------------------------------------
UPDATE tbl_monthly_report
SET status = 'READY',
    pdf_object_key = ?,
    pdf_size_bytes = ?,
    share_token = ?,  -- 32자 랜덤 토큰
    share_expires_at = DATE_ADD(NOW(6), INTERVAL 30 DAY),
    llm_invocation_id = ?,
    total_cost_usd = ?,
    generated_at = NOW(6),
    updated_at = NOW(6)
WHERE report_id = ?;


-- ----------------------------------------------------------------------------
-- 4.6 리포트 실패 처리
-- ----------------------------------------------------------------------------
UPDATE tbl_monthly_report
SET status = 'FAILED',
    failed_reason = ?,
    updated_at = NOW(6)
WHERE report_id = ?;


-- ----------------------------------------------------------------------------
-- 4.7 사용자의 리포트 목록 조회 (앱 내 리포트 탭)
-- ----------------------------------------------------------------------------
-- 최근 12개월 리포트
SELECT
    BIN_TO_UUID(report_id, 1) AS report_id,
    period_month,
    status,
    generated_at,
    delivered_at,
    pdf_object_key IS NOT NULL AS has_pdf
FROM tbl_monthly_report
WHERE user_id = ?
  AND status IN ('READY', 'DELIVERED')
ORDER BY period_month DESC
LIMIT 12;


-- ----------------------------------------------------------------------------
-- 4.8 특정 리포트 상세 조회 (섹션 포함)
-- ----------------------------------------------------------------------------
SELECT
    r.period_month,
    r.status,
    r.generated_at,
    r.pdf_object_key,
    s.section_type,
    s.display_order,
    s.easy_read_text,
    s.traffic_light,
    s.action_label,
    s.action_payload,
    s.raw_data
FROM tbl_monthly_report r
INNER JOIN tbl_report_section s ON s.report_id = r.report_id
WHERE r.report_id = ?
  AND r.user_id = ?   -- 본인 확인
ORDER BY s.display_order;


-- ----------------------------------------------------------------------------
-- 4.9 자녀 공유 링크로 리포트 접근 (share_token 검증)
-- ----------------------------------------------------------------------------
-- 부모가 발급한 공유 토큰으로 자녀가 접근하는 경우
SELECT
    BIN_TO_UUID(r.report_id, 1) AS report_id,
    BIN_TO_UUID(r.user_id, 1) AS senior_user_id,
    r.period_month,
    r.pdf_object_key,
    r.share_expires_at
FROM tbl_monthly_report r
WHERE r.share_token = ?
  AND r.share_expires_at > NOW(6)
  AND r.status IN ('READY', 'DELIVERED');


-- ----------------------------------------------------------------------------
-- 4.10 리포트 전달 상태 업데이트 (Push 발송 완료 시)
-- ----------------------------------------------------------------------------
UPDATE tbl_monthly_report
SET status = 'DELIVERED',
    delivered_at = NOW(6),
    updated_at = NOW(6)
WHERE report_id = ?
  AND status = 'READY';


-- ----------------------------------------------------------------------------
-- 4.11 LLM 비용 집계 (월별 운영 지표)
-- ----------------------------------------------------------------------------
-- 이번 달 리포트 생성에 들어간 AI 비용
SELECT
    DATE_FORMAT(period_month, '%Y-%m') AS month,
    COUNT(*) AS report_count,
    SUM(total_cost_usd) AS total_cost_usd,
    AVG(total_cost_usd) AS avg_cost_per_report,
    COUNT(CASE WHEN status = 'FAILED' THEN 1 END) AS failed_count
FROM tbl_monthly_report
WHERE period_month >= DATE_SUB(CURDATE(), INTERVAL 6 MONTH)
GROUP BY month
ORDER BY month DESC;
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
-- ============================================================================
-- USE CASE 06. 운영 모니터링 & 대시보드
-- ----------------------------------------------------------------------------
-- 시나리오: 운영팀이 Grafana 대시보드, 장애 대응, KPI 추적
-- 빈도: 대시보드 새로고침 시마다 (분 단위)
-- 핵심: 집계 쿼리 성능, 인덱스 활용, 필요시 materialized view
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 6.1 실시간 KPI - 현재 시점 사용자 & 자산 현황
-- ----------------------------------------------------------------------------
-- 대시보드 상단 요약 카드용
SELECT
    (SELECT COUNT(*) FROM tbl_user WHERE status = 'ACTIVE') AS active_users,
    (SELECT COUNT(*) FROM tbl_user
     WHERE status = 'ACTIVE'
       AND created_at >= DATE_SUB(NOW(6), INTERVAL 30 DAY)) AS new_users_30d,
    (SELECT COUNT(*) FROM tbl_linked_institution WHERE status = 'ACTIVE') AS active_links,
    (SELECT COUNT(*) FROM tbl_account WHERE closed_at IS NULL) AS total_accounts,
    (SELECT COALESCE(SUM(last_balance), 0) FROM tbl_account
     WHERE closed_at IS NULL) AS total_aum;  -- Assets Under Management


-- ----------------------------------------------------------------------------
-- 6.2 시간대별 이체 성공률 (최근 24시간)
-- ----------------------------------------------------------------------------
-- 성능 목표: < 200ms (idx_transfer_user_status 활용, created_at 범위 스캔)
SELECT
    DATE_FORMAT(created_at, '%Y-%m-%d %H:00:00') AS hour,
    COUNT(*) AS total_orders,
    SUM(CASE WHEN status = 'EXECUTED' THEN 1 ELSE 0 END) AS success_count,
    SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) AS fail_count,
    SUM(CASE WHEN status = 'CANCELLED' THEN 1 ELSE 0 END) AS cancel_count,
    ROUND(
        100.0 * SUM(CASE WHEN status = 'EXECUTED' THEN 1 ELSE 0 END)
        / NULLIF(COUNT(*), 0)
    , 2) AS success_rate_pct
FROM tbl_transfer_order
WHERE created_at >= DATE_SUB(NOW(6), INTERVAL 24 HOUR)
GROUP BY hour
ORDER BY hour DESC;


-- ----------------------------------------------------------------------------
-- 6.3 FAILED 이체의 실패 원인 분석 (최근 7일)
-- ----------------------------------------------------------------------------
-- 상위 실패 패턴 파악
SELECT
    o.failed_reason,
    COUNT(*) AS fail_count,
    SUM(o.amount) AS total_failed_amount,
    AVG(TIMESTAMPDIFF(SECOND, o.created_at, o.updated_at)) AS avg_fail_latency_sec
FROM tbl_transfer_order o
WHERE o.status = 'FAILED'
  AND o.created_at >= DATE_SUB(NOW(6), INTERVAL 7 DAY)
GROUP BY o.failed_reason
ORDER BY fail_count DESC
LIMIT 20;


-- ----------------------------------------------------------------------------
-- 6.4 마이데이터 연동 에러율 (기관별)
-- ----------------------------------------------------------------------------
-- 특정 금융기관 API 장애 감지용
SELECT
    institution_code,
    institution_name,
    institution_type,
    COUNT(*) AS total_links,
    SUM(CASE WHEN status = 'ERROR' THEN 1 ELSE 0 END) AS error_count,
    SUM(CASE WHEN status = 'EXPIRED' THEN 1 ELSE 0 END) AS expired_count,
    SUM(CASE WHEN last_error_at >= DATE_SUB(NOW(6), INTERVAL 1 HOUR) THEN 1 ELSE 0 END)
        AS recent_error_1h,
    ROUND(
        100.0 * SUM(CASE WHEN status = 'ERROR' THEN 1 ELSE 0 END) / NULLIF(COUNT(*), 0)
    , 2) AS error_rate_pct
FROM tbl_linked_institution
GROUP BY institution_code, institution_name, institution_type
HAVING error_count > 0 OR recent_error_1h > 0
ORDER BY error_rate_pct DESC, error_count DESC;


-- ----------------------------------------------------------------------------
-- 6.5 일간 활성 사용자 (DAU) 집계
-- ----------------------------------------------------------------------------
SELECT
    DATE(last_login_at) AS login_date,
    COUNT(DISTINCT user_id) AS dau
FROM tbl_user
WHERE last_login_at >= DATE_SUB(NOW(6), INTERVAL 30 DAY)
  AND status = 'ACTIVE'
GROUP BY login_date
ORDER BY login_date DESC;


-- ----------------------------------------------------------------------------
-- 6.6 시니어 UX 모드 전환율
-- ----------------------------------------------------------------------------
-- 전체 사용자 중 시니어 모드 비율 + 연령대별 분포
SELECT
    up.age_range,
    COUNT(*) AS total_users,
    SUM(CASE WHEN up.ux_mode = 'SENIOR' THEN 1 ELSE 0 END) AS senior_mode_users,
    SUM(CASE WHEN up.voice_enabled = TRUE THEN 1 ELSE 0 END) AS voice_enabled_users,
    AVG(up.font_scale) AS avg_font_scale,
    ROUND(
        100.0 * SUM(CASE WHEN up.ux_mode = 'SENIOR' THEN 1 ELSE 0 END)
        / NULLIF(COUNT(*), 0)
    , 2) AS senior_mode_pct
FROM tbl_user_profile up
INNER JOIN tbl_user u ON u.user_id = up.user_id
WHERE u.status = 'ACTIVE'
GROUP BY up.age_range
ORDER BY up.age_range;


-- ----------------------------------------------------------------------------
-- 6.7 가족 공유 활성화율
-- ----------------------------------------------------------------------------
-- 핵심 차별화 기능의 실제 사용률
SELECT
    COUNT(DISTINCT u.user_id) AS total_active_users,
    COUNT(DISTINCT gl_as_senior.senior_user_id) AS seniors_with_guardian,
    COUNT(DISTINCT gl_as_guardian.guardian_user_id) AS guardians_connected,
    ROUND(
        100.0 * COUNT(DISTINCT gl_as_senior.senior_user_id)
        / NULLIF(COUNT(DISTINCT u.user_id), 0)
    , 2) AS family_share_rate_pct
FROM tbl_user u
LEFT JOIN tbl_guardian_link gl_as_senior
    ON gl_as_senior.senior_user_id = u.user_id AND gl_as_senior.status = 'ACTIVE'
LEFT JOIN tbl_guardian_link gl_as_guardian
    ON gl_as_guardian.guardian_user_id = u.user_id AND gl_as_guardian.status = 'ACTIVE'
WHERE u.status = 'ACTIVE';


-- ----------------------------------------------------------------------------
-- 6.8 리포트 생성 배치 지연 감지 (SLA 모니터링)
-- ----------------------------------------------------------------------------
-- 매월 1일 생성 배치가 제때 끝났는지 확인
SELECT
    DATE_FORMAT(period_month, '%Y-%m') AS month,
    COUNT(*) AS total_reports,
    SUM(CASE WHEN status = 'GENERATING' THEN 1 ELSE 0 END) AS still_generating,
    SUM(CASE WHEN status = 'READY' THEN 1 ELSE 0 END) AS ready,
    SUM(CASE WHEN status = 'DELIVERED' THEN 1 ELSE 0 END) AS delivered,
    SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) AS failed,
    AVG(TIMESTAMPDIFF(SECOND, created_at, generated_at)) AS avg_generation_sec,
    MAX(TIMESTAMPDIFF(SECOND, created_at, generated_at)) AS max_generation_sec
FROM tbl_monthly_report
WHERE period_month >= DATE_SUB(CURDATE(), INTERVAL 3 MONTH)
GROUP BY month
ORDER BY month DESC;


-- ----------------------------------------------------------------------------
-- 6.9 푸시 알림 전달 성공률
-- ----------------------------------------------------------------------------
SELECT
    DATE(created_at) AS date,
    notification_type,
    COUNT(*) AS total_sent,
    SUM(CASE WHEN status IN ('SENT', 'DELIVERED', 'READ') THEN 1 ELSE 0 END) AS success,
    SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) AS failed,
    SUM(CASE WHEN status = 'READ' THEN 1 ELSE 0 END) AS read_count,
    ROUND(
        100.0 * SUM(CASE WHEN status = 'READ' THEN 1 ELSE 0 END) / NULLIF(COUNT(*), 0)
    , 2) AS read_rate_pct
FROM tbl_notification
WHERE created_at >= DATE_SUB(NOW(6), INTERVAL 7 DAY)
GROUP BY date, notification_type
ORDER BY date DESC, notification_type;


-- ----------------------------------------------------------------------------
-- 6.10 Suspicious 이체 감지 (일일 리포트용)
-- ----------------------------------------------------------------------------
-- 보이스피싱 의심 사례 대응 로그
SELECT
    DATE(o.created_at) AS date,
    COUNT(*) AS total_transfers,
    SUM(CASE WHEN TIME(o.created_at) BETWEEN '22:00:00' AND '06:00:00'
             THEN 1 ELSE 0 END) AS late_night_count,
    SUM(CASE WHEN o.amount >= 10000000 THEN 1 ELSE 0 END) AS high_amount_count,
    SUM(CASE WHEN o.status = 'CANCELLED'
              AND TIMESTAMPDIFF(SECOND, o.created_at, o.updated_at) < 60
             THEN 1 ELSE 0 END) AS quick_cancel_count
FROM tbl_transfer_order o
WHERE o.created_at >= DATE_SUB(NOW(6), INTERVAL 7 DAY)
GROUP BY date
ORDER BY date DESC;


-- ----------------------------------------------------------------------------
-- 6.11 이체 지연 분석 (p50, p95, p99 latency)
-- ----------------------------------------------------------------------------
-- EXECUTED 기준으로 create → execute 소요 시간 분위수
-- MariaDB 10.3+ / MySQL 8.0+ window function 활용
WITH transfer_latency AS (
    SELECT
        TIMESTAMPDIFF(MICROSECOND, created_at, executed_at) / 1000000 AS latency_sec
    FROM tbl_transfer_order
    WHERE status = 'EXECUTED'
      AND executed_at >= DATE_SUB(NOW(6), INTERVAL 24 HOUR)
),
percentiles AS (
    SELECT
        latency_sec,
        PERCENT_RANK() OVER (ORDER BY latency_sec) AS pct_rank
    FROM transfer_latency
)
SELECT
    MIN(CASE WHEN pct_rank >= 0.50 THEN latency_sec END) AS p50_sec,
    MIN(CASE WHEN pct_rank >= 0.95 THEN latency_sec END) AS p95_sec,
    MIN(CASE WHEN pct_rank >= 0.99 THEN latency_sec END) AS p99_sec,
    MAX(latency_sec) AS max_sec
FROM percentiles;
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
