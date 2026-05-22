-- Store the frontend origin that initiated OAuth login.
-- The backend BFF callback appends burty.auth.oauth-success-redirect
-- (default: /auth/callback) to this origin after issuing auth cookies.

ALTER TABLE tbl_oauth_state
    ADD COLUMN IF NOT EXISTS frontend_origin VARCHAR(255) NULL COMMENT 'OAuth 완료 후 돌아갈 FE origin'
    AFTER provider;

ALTER TABLE tbl_oauth_state
    DROP COLUMN IF EXISTS frontend_redirect_uri,
    DROP COLUMN IF EXISTS redirect_uri;
