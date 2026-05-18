-- tbl_oauth_state: authorize 단계 redirect_uri 저장 (BFF callback 토큰 교환용)
ALTER TABLE tbl_oauth_state
    ADD COLUMN IF NOT EXISTS redirect_uri VARCHAR(512) NULL AFTER provider;
