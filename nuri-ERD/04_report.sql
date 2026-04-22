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
