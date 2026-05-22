-- Fix a mixed schema created by Hibernate ddl-auto=update after renaming PII columns.
--
-- Symptom:
--   Field 'ci_encrypted' doesn't have a default value
--   insert into tbl_user (ci, ci_hash, ..., phone, phone_hash, ...)
--
-- Cause:
--   New plain columns such as ci/phone were added, but old NOT NULL columns
--   such as ci_encrypted/phone_encrypted still remain.
--
-- Run this after confirming the application is deployed with the plain column names.

ALTER TABLE tbl_user
    DROP COLUMN IF EXISTS ci_encrypted,
    DROP COLUMN IF EXISTS phone_encrypted;

ALTER TABLE tbl_user_profile
    DROP COLUMN IF EXISTS name_encrypted,
    DROP COLUMN IF EXISTS birthdate_encrypted;

ALTER TABLE tbl_social_account
    DROP COLUMN IF EXISTS email_encrypted,
    DROP COLUMN IF EXISTS display_name_encrypted;
