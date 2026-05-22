-- Plaintext storage: drop legacy *_encrypted / hash+mask columns where entities now use plain fields.
-- Run after deploying the application build that maps plain column names.
-- Safe to re-run: uses IF EXISTS / IF NOT EXISTS (MariaDB 10.5+).

-- tbl_registered_account
ALTER TABLE tbl_registered_account
    ADD COLUMN IF NOT EXISTS account_no VARCHAR(80) NULL AFTER user_id;

ALTER TABLE tbl_registered_account
    DROP COLUMN IF EXISTS account_no_hash,
    DROP COLUMN IF EXISTS account_no_encrypted,
    DROP COLUMN IF EXISTS account_no_masked;

ALTER TABLE tbl_registered_account
    MODIFY COLUMN account_no VARCHAR(80) NOT NULL;

-- tbl_device
ALTER TABLE tbl_device
    ADD COLUMN IF NOT EXISTS device_token VARCHAR(500) NULL AFTER device_token_hash;

ALTER TABLE tbl_device
    DROP COLUMN IF EXISTS device_token_encrypted;

ALTER TABLE tbl_device
    MODIFY COLUMN device_token VARCHAR(500) NOT NULL;

-- tbl_account
ALTER TABLE tbl_account
    ADD COLUMN IF NOT EXISTS account_no VARCHAR(80) NULL AFTER link_id;

ALTER TABLE tbl_account
    DROP COLUMN IF EXISTS account_no_encrypted;

ALTER TABLE tbl_account
    MODIFY COLUMN account_no VARCHAR(80) NOT NULL;

-- tbl_linked_institution
ALTER TABLE tbl_linked_institution
    ADD COLUMN IF NOT EXISTS access_token VARCHAR(2000) NULL,
    ADD COLUMN IF NOT EXISTS refresh_token VARCHAR(2000) NULL;

ALTER TABLE tbl_linked_institution
    DROP COLUMN IF EXISTS access_token_encrypted,
    DROP COLUMN IF EXISTS refresh_token_encrypted;

ALTER TABLE tbl_linked_institution
    MODIFY COLUMN access_token VARCHAR(2000) NOT NULL,
    MODIFY COLUMN refresh_token VARCHAR(2000) NOT NULL;

-- tbl_transfer_order
ALTER TABLE tbl_transfer_order
    ADD COLUMN IF NOT EXISTS to_account_no VARCHAR(80) NULL AFTER from_account_id;

ALTER TABLE tbl_transfer_order
    DROP COLUMN IF EXISTS to_account_no_encrypted;

ALTER TABLE tbl_transfer_order
    MODIFY COLUMN to_account_no VARCHAR(80) NOT NULL;
