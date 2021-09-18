ALTER TABLE attachment
    ADD COLUMN message_content_id UUID,
    ADD COLUMN message_draft_id UUID,
    ADD CONSTRAINT attachment_message_content_id_fkey FOREIGN KEY (message_content_id) REFERENCES message_content (id) ON DELETE RESTRICT,
    ADD CONSTRAINT attachment_message_draft_id_fkey FOREIGN KEY (message_draft_id) REFERENCES message_draft (id) ON DELETE CASCADE,
    DROP CONSTRAINT created_for_fk,
    ADD CONSTRAINT created_for_fk CHECK (num_nonnulls(application_id, income_statement_id, message_content_id, message_draft_id) <= 1);
