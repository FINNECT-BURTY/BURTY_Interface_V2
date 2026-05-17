-- Convert the core user primary key from UUID/BINARY(16) to numeric AUTO_INCREMENT.
-- Intended for the current empty production schema. If rows exist, add an explicit
-- id mapping step before running this migration.

USE burty;

ALTER TABLE tbl_device DROP FOREIGN KEY FKgd3pehc4cpxsm0vdyexsfx1f8;
ALTER TABLE tbl_linked_institution DROP FOREIGN KEY FKmgmu0961eplmvsndfiu57pmk9;
ALTER TABLE tbl_user_profile DROP FOREIGN KEY FK4wgwevksr5cpfhm8hv4b2xs70;
ALTER TABLE tbl_spending_limit DROP FOREIGN KEY FKidkrvaffh5ml3ld16sswyy12r;
ALTER TABLE tbl_biometric_credential DROP FOREIGN KEY FKcyhinctj2ly1xqiws7r03und1;
ALTER TABLE tbl_consent_record DROP FOREIGN KEY FK1r7qg0irovs3cn802utq3sm6y;
ALTER TABLE tbl_guardian_link DROP FOREIGN KEY FKi3m3i1648mvvnjq3y1n7xmnbv;
ALTER TABLE tbl_guardian_link DROP FOREIGN KEY FKimepx024scpmre84f8gcsylgs;
ALTER TABLE tbl_notification DROP FOREIGN KEY FKkpuv1ixu8pkufic0xe8bcrqil;
ALTER TABLE tbl_transfer_order DROP FOREIGN KEY FKg6oo1y257pil1ws7v3weg1bpb;
ALTER TABLE tbl_monthly_report DROP FOREIGN KEY FKq3bokidnnaiohsvx2hh9rhui7;

ALTER TABLE tbl_user MODIFY user_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE tbl_user_profile MODIFY user_id BIGINT NOT NULL;
ALTER TABLE tbl_device MODIFY user_id BIGINT NOT NULL;
ALTER TABLE tbl_linked_institution MODIFY user_id BIGINT NOT NULL;
ALTER TABLE tbl_spending_limit MODIFY user_id BIGINT NOT NULL;
ALTER TABLE tbl_biometric_credential MODIFY user_id BIGINT NOT NULL;
ALTER TABLE tbl_consent_record MODIFY user_id BIGINT NOT NULL;
ALTER TABLE tbl_guardian_link MODIFY senior_user_id BIGINT NOT NULL;
ALTER TABLE tbl_guardian_link MODIFY guardian_user_id BIGINT NOT NULL;
ALTER TABLE tbl_notification MODIFY recipient_user_id BIGINT NOT NULL;
ALTER TABLE tbl_transfer_order MODIFY user_id BIGINT NOT NULL;
ALTER TABLE tbl_monthly_report MODIFY user_id BIGINT NOT NULL;
ALTER TABLE tbl_social_account MODIFY user_id BIGINT NOT NULL;

ALTER TABLE tbl_device
    ADD CONSTRAINT FK_tbl_device_user FOREIGN KEY (user_id) REFERENCES tbl_user(user_id);
ALTER TABLE tbl_linked_institution
    ADD CONSTRAINT FK_tbl_linked_institution_user FOREIGN KEY (user_id) REFERENCES tbl_user(user_id);
ALTER TABLE tbl_user_profile
    ADD CONSTRAINT FK_tbl_user_profile_user FOREIGN KEY (user_id) REFERENCES tbl_user(user_id);
ALTER TABLE tbl_spending_limit
    ADD CONSTRAINT FK_tbl_spending_limit_user FOREIGN KEY (user_id) REFERENCES tbl_user(user_id);
ALTER TABLE tbl_biometric_credential
    ADD CONSTRAINT FK_tbl_biometric_credential_user FOREIGN KEY (user_id) REFERENCES tbl_user(user_id);
ALTER TABLE tbl_consent_record
    ADD CONSTRAINT FK_tbl_consent_record_user FOREIGN KEY (user_id) REFERENCES tbl_user(user_id);
ALTER TABLE tbl_guardian_link
    ADD CONSTRAINT FK_tbl_guardian_link_senior_user FOREIGN KEY (senior_user_id) REFERENCES tbl_user(user_id);
ALTER TABLE tbl_guardian_link
    ADD CONSTRAINT FK_tbl_guardian_link_guardian_user FOREIGN KEY (guardian_user_id) REFERENCES tbl_user(user_id);
ALTER TABLE tbl_notification
    ADD CONSTRAINT FK_tbl_notification_recipient_user FOREIGN KEY (recipient_user_id) REFERENCES tbl_user(user_id);
ALTER TABLE tbl_transfer_order
    ADD CONSTRAINT FK_tbl_transfer_order_user FOREIGN KEY (user_id) REFERENCES tbl_user(user_id);
ALTER TABLE tbl_monthly_report
    ADD CONSTRAINT FK_tbl_monthly_report_user FOREIGN KEY (user_id) REFERENCES tbl_user(user_id);
