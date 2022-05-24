ALTER TABLE decision DROP CONSTRAINT created_by;
ALTER TABLE decision ADD CONSTRAINT fk$created_by FOREIGN KEY (created_by) REFERENCES evaka_user (id);


ALTER TABLE old_service_need DROP CONSTRAINT created_by; -- yes, this is the actual name
ALTER TABLE old_service_need ADD CONSTRAINT fk$updated_by FOREIGN KEY (updated_by) REFERENCES evaka_user (id);


ALTER TABLE application_note DROP CONSTRAINT fk$created_by;
ALTER TABLE application_note ADD CONSTRAINT fk$created_by FOREIGN KEY (created_by) REFERENCES evaka_user (id);


ALTER TABLE application_note DROP CONSTRAINT fk$updated_by;
ALTER TABLE application_note ADD CONSTRAINT fk$updated_by FOREIGN KEY (updated_by) REFERENCES evaka_user (id);


ALTER TABLE assistance_action DROP CONSTRAINT fk$updated_by;
ALTER TABLE assistance_action ADD CONSTRAINT fk$updated_by FOREIGN KEY (updated_by) REFERENCES evaka_user (id);


ALTER TABLE service_need DROP CONSTRAINT new_service_need_confirmed_by_fkey;
ALTER TABLE service_need ADD CONSTRAINT fk$confirmed_by FOREIGN KEY (confirmed_by) REFERENCES evaka_user (id);


ALTER TABLE pedagogical_document DROP CONSTRAINT pedagogical_document_created_by_fkey;
ALTER TABLE pedagogical_document ADD CONSTRAINT fk$created_by FOREIGN KEY (created_by) REFERENCES evaka_user (id);


ALTER TABLE pedagogical_document DROP CONSTRAINT pedagogical_document_updated_by_fkey;
ALTER TABLE pedagogical_document ADD CONSTRAINT fk$updated_by FOREIGN KEY (updated_by) REFERENCES evaka_user (id);


ALTER TABLE invoice DROP CONSTRAINT sent_by;
ALTER TABLE invoice ADD CONSTRAINT fk$sent_by FOREIGN KEY (sent_by) REFERENCES evaka_user (id);


ALTER TABLE assistance_need DROP CONSTRAINT updated_by;
ALTER TABLE assistance_need ADD CONSTRAINT fk$updated_by FOREIGN KEY (updated_by) REFERENCES evaka_user (id);


ALTER TABLE fee_alteration DROP CONSTRAINT updated_by;
ALTER TABLE fee_alteration ADD CONSTRAINT fk$updated_by FOREIGN KEY (updated_by) REFERENCES evaka_user (id);


ALTER TABLE attachment ADD COLUMN uploaded_by uuid CONSTRAINT fk$uploaded_by REFERENCES evaka_user (id);
UPDATE attachment SET uploaded_by = coalesce(uploaded_by_employee, uploaded_by_person, '00000000-0000-0000-0000-000000000000');
ALTER TABLE attachment
    DROP COLUMN uploaded_by_employee,
    DROP COLUMN uploaded_by_person,
    ALTER COLUMN uploaded_by SET NOT NULL;


ALTER TABLE attendance_reservation ADD COLUMN created_by uuid CONSTRAINT fk$created_by REFERENCES evaka_user (id);
UPDATE attendance_reservation SET created_by = coalesce(created_by_employee_id, created_by_guardian_id, '00000000-0000-0000-0000-000000000000');
ALTER TABLE attendance_reservation
    DROP COLUMN created_by_employee_id,
    DROP COLUMN created_by_guardian_id,
    ALTER COLUMN created_by SET NOT NULL;
