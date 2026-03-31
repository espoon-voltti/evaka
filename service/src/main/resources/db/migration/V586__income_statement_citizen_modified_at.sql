ALTER TABLE income_statement ADD COLUMN citizen_modified_at timestamp with time zone;

UPDATE income_statement SET citizen_modified_at = sent_at WHERE sent_at IS NOT NULL;

CREATE INDEX idx$income_statement_citizen_modified_at_awaiting_handler
    ON income_statement (citizen_modified_at DESC NULLS LAST)
    WHERE status = 'SENT'::income_statement_status
       OR status = 'HANDLING'::income_statement_status;

DROP INDEX idx$income_statement_sent_at_awaiting_handler;
CREATE INDEX idx$income_statement_sent_at_awaiting_handler
    ON income_statement (sent_at)
    WHERE status = 'SENT'::income_statement_status
       OR status = 'HANDLING'::income_statement_status;
