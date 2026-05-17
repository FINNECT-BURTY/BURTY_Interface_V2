-- Rename PII columns from encrypted naming to plain/original-data naming.
-- Existing encrypted values cannot be decrypted by this DDL. For already-created rows,
-- replace values through application flows or a separate backfill if plaintext is available.

ALTER TABLE tbl_user
    CHANGE COLUMN ci_encrypted ci VARCHAR(255) NOT NULL COMMENT 'CI 원본',
    CHANGE COLUMN phone_encrypted phone VARCHAR(20) NOT NULL COMMENT '전화번호 원본';

ALTER TABLE tbl_user_profile
    CHANGE COLUMN name_encrypted name VARCHAR(100) NOT NULL COMMENT '실명 원본',
    CHANGE COLUMN birthdate_encrypted birthdate DATE NOT NULL COMMENT '생년월일 원본';

ALTER TABLE tbl_social_account
    CHANGE COLUMN email_encrypted email VARCHAR(255) NULL COMMENT '소셜 이메일 원본',
    CHANGE COLUMN display_name_encrypted display_name VARCHAR(100) NULL COMMENT '소셜 표시 이름 원본';
