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
