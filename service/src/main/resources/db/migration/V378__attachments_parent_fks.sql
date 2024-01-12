-- re-create all fk constraints with ON DELETE SET NULL so deleting the parent makes the attachment an orphan
ALTER TABLE attachment
    DROP CONSTRAINT attachment_application_id_fkey,
    DROP CONSTRAINT attachment_fee_alteration_id_fkey,
    DROP CONSTRAINT attachment_income_id_fkey,
    DROP CONSTRAINT attachment_income_statement_id_fkey,
    DROP CONSTRAINT attachment_message_content_id_fkey,
    DROP CONSTRAINT attachment_message_draft_id_fkey,
    DROP CONSTRAINT attachment_pedagogical_document_id_fkey;
ALTER TABLE attachment
    ADD CONSTRAINT fk$application FOREIGN KEY (application_id) REFERENCES application(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk$fee_alteration FOREIGN KEY (fee_alteration_id) REFERENCES fee_alteration(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk$income FOREIGN KEY (income_id) REFERENCES income(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk$income_statement FOREIGN KEY (income_statement_id) REFERENCES income_statement(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk$message_content FOREIGN KEY (message_content_id) REFERENCES message_content(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk$message_draft FOREIGN KEY (message_draft_id) REFERENCES message_draft(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk$pedagogical_document FOREIGN KEY (pedagogical_document_id) REFERENCES pedagogical_document(id) ON DELETE SET NULL;

-- fix check constraint which was missing some foreign key columns
ALTER TABLE attachment
    DROP CONSTRAINT created_for_fk,
    ADD CONSTRAINT created_for_fk CHECK (num_nonnulls(application_id, fee_alteration_id, income_id, income_statement_id, message_content_id, message_draft_id, pedagogical_document_id) <= 1);
