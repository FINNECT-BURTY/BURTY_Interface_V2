-- 감사 로그 무결성 (해시 체인)
--
-- 감사 로그는 "무슨 일이 있었는지" 를 사후에 증명하는 기록인데, 단순 INSERT 만으로는
-- DB 접근 권한이 있는 사람이 조용히 지우거나 고칠 수 있었다.
-- 각 행이 직전 행의 해시를 품게 하면 중간 한 행만 손대도 이후 체인이 전부 어긋난다.

ALTER TABLE tbl_audit_log
    ADD COLUMN IF NOT EXISTS prev_hash CHAR(64) NULL,
    ADD COLUMN IF NOT EXISTS entry_hash CHAR(64) NULL,
    ADD COLUMN IF NOT EXISTS chain_seq BIGINT NULL;

-- 체인 순번은 유일해야 한다. 중복이 생기면 삭제 탐지가 무력해진다.
CREATE UNIQUE INDEX IF NOT EXISTS uk_audit_chain_seq
    ON tbl_audit_log (chain_seq);

-- 체인 도입 이전 행들은 chain_seq 가 NULL 로 남아 검증에서 제외된다.
-- (소급 계산은 불가능하다. 그 시점 이전 기록의 무결성은 증명할 수 없다는 사실 자체를 남긴다.)
