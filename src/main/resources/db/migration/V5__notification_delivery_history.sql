-- 알림 발송 이력 보강
--
-- 기존에는 tbl_notification 에 QUEUED 로 저장만 되고 상태가 영원히 바뀌지 않았다.
-- "보냈다" 는 사실이 애플리케이션 로그에만 남아서, 사용자가 "알림이 안 왔다" 고 할 때
-- 시도했는지 / 몇 번 실패했는지 / 어떤 채널로 보냈는지 조회할 방법이 없었다.

ALTER TABLE tbl_notification
    ADD COLUMN IF NOT EXISTS attempts INT NOT NULL DEFAULT 0 AFTER failed_reason,
    ADD COLUMN IF NOT EXISTS delivery_channel VARCHAR(20) NULL AFTER attempts;

-- 발송 대기/실패 건을 운영에서 빠르게 찾기 위한 인덱스.
CREATE INDEX IF NOT EXISTS idx_notification_status
    ON tbl_notification (status, notification_id);
