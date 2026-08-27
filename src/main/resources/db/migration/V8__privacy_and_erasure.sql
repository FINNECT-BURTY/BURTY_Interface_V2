-- 개인정보 파기 요청 추적
--
-- 탈퇴 처리는 한 번에 끝나지 않는다.
--   - 직접 식별정보(CI, 전화번호, 이름, 생년월일)는 즉시 익명화한다.
--   - 전자금융거래 기록과 감사 로그는 법정 보존의무가 있어 즉시 지울 수 없다.
-- 그래서 "언제 무엇을 지웠고 무엇이 언제까지 남는지" 를 이 테이블이 기록한다.
-- 이 기록 자체가 정보주체의 파기 요청에 대한 처리 증빙이 된다.

CREATE TABLE IF NOT EXISTS tbl_data_erasure_request (
    erasure_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    reason VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'IMMEDIATE_DONE',
    requested_at DATETIME(6) NOT NULL,
    anonymized_at DATETIME(6) NULL,
    retention_until DATETIME(6) NOT NULL,
    purged_at DATETIME(6) NULL,
    summary VARCHAR(1000) NULL,
    PRIMARY KEY (erasure_id)
) ENGINE=InnoDB COMMENT='개인정보 파기 요청/처리 이력';

CREATE INDEX IF NOT EXISTS idx_erasure_user ON tbl_data_erasure_request (user_id);
CREATE INDEX IF NOT EXISTS idx_erasure_due ON tbl_data_erasure_request (status, retention_until);
