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
