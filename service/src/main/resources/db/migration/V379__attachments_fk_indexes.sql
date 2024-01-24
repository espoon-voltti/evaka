-- Re-create foreign key indexes as partial indexes, because there's no point in indexing nulls
DROP INDEX attachment_application_id_idx;
DROP INDEX idx$attachment_fee_alteration;
DROP INDEX idx$attachment_income;
DROP INDEX idx$attachment_income_statement;
DROP INDEX idx$attachment_message_content;
DROP INDEX idx$attachment_message_draft;
-- attachment_pedagogic_document is not dropped and re-created because it's already a partial index
CREATE INDEX idx$attachment_application ON attachment (application_id) WHERE application_id IS NOT NULL;
CREATE INDEX idx$attachment_fee_alteration ON attachment (fee_alteration_id) WHERE fee_alteration_id IS NOT NULL;
CREATE INDEX idx$attachment_income ON attachment (income_id) WHERE income_id IS NOT NULL;
CREATE INDEX idx$attachment_income_statement ON attachment (income_statement_id) WHERE income_statement_id IS NOT NULL;
CREATE INDEX idx$attachment_message_content ON attachment (message_content_id) WHERE message_content_id IS NOT NULL;
CREATE INDEX idx$attachment_message_draft ON attachment (message_draft_id) WHERE message_draft_id IS NOT NULL;

-- A separate index can be used to quickly find orphan attachments
CREATE INDEX idx$attachment_orphan ON attachment (id)
    WHERE application_id IS NULL
    AND fee_alteration_id IS NULL
    AND income_id IS NULL
    AND income_statement_id IS NULL
    AND message_content_id IS NULL
    AND message_draft_id IS NULL
    AND pedagogical_document_id IS NULL;
