ALTER TABLE attachment
    ADD COLUMN income_id UUID,
    ADD CONSTRAINT attachment_income_id_fkey FOREIGN KEY (income_id) REFERENCES income (id) ON DELETE RESTRICT,
    DROP CONSTRAINT created_for_fk,
    ADD CONSTRAINT created_for_fk CHECK (num_nonnulls(application_id, income_statement_id, income_id, message_content_id, message_draft_id) <= 1);

CREATE INDEX idx$attachment_income ON attachment (income_id);
