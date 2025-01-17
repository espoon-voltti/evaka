ALTER TABLE attachment
    ADD COLUMN invoice_id uuid,
    ADD CONSTRAINT fk$invoice FOREIGN KEY (invoice_id) REFERENCES invoice(id) ON DELETE SET NULL;

ALTER TABLE attachment DROP CONSTRAINT created_for_fk;
ALTER TABLE attachment ADD CONSTRAINT created_for_fk CHECK (
    num_nonnulls(
        application_id, fee_alteration_id, income_id, income_statement_id,
        invoice_id, message_content_id, message_draft_id, pedagogical_document_id
    ) <= 1
);

DROP INDEX idx$attachment_orphan;
CREATE INDEX idx$attachment_orphan ON attachment(id) WHERE
    application_id IS NULL AND
    fee_alteration_id IS NULL AND
    income_id IS NULL AND
    income_statement_id IS NULL AND
    invoice_id IS NULL AND
    message_content_id IS NULL AND
    message_draft_id IS NULL AND
    pedagogical_document_id IS NULL;

CREATE INDEX idx$attachment_invoice ON attachment(invoice_id) WHERE invoice_id IS NOT NULL;
