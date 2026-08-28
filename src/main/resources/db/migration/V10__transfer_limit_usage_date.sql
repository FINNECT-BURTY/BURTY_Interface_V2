-- 한도를 실제로 차감한 날짜를 이체 주문에 기록
--
-- 일일 한도 사용량은 (user_id, usage_date) 로 집계된다. 지금까지 해제할 때는
-- requested_at 에서 날짜를 다시 계산했는데, 주문 생성과 한도 예약 사이에 자정이 지나면
-- 예약한 행과 다른 날짜를 가리켜 해제가 조용히 실패했다.
-- (돈이 새지는 않지만 사용자 한도가 하루 동안 복구되지 않는다.)
--
-- 차감한 날짜를 그대로 들고 있으면 재계산이 필요 없다.

ALTER TABLE tbl_transfer_order
    ADD COLUMN IF NOT EXISTS limit_usage_date DATE NULL AFTER reconcile_attempts;

-- 기존 행은 requested_at 기준으로 채운다. 완벽하지는 않지만(자정 경계 건은 여전히 어긋날 수
-- 있다) 지금까지 쓰던 값과 동일하므로 동작이 나빠지지는 않는다.
-- 아직 확정되지 않은 건만 대상으로 한다 — 확정된 건은 해제할 일이 없다.
UPDATE tbl_transfer_order
SET limit_usage_date = DATE(requested_at)
WHERE limit_usage_date IS NULL
  AND status IN ('AUTH_REQUESTED', 'AUTHORIZED', 'EXECUTING', 'UNKNOWN');
