CREATE TYPE income_statement_status AS ENUM ('DRAFT', 'SENT', 'HANDLED');

ALTER TABLE income_statement
    ADD COLUMN status income_statement_status,
    ADD COLUMN sent_at TIMESTAMP WITH TIME ZONE;

UPDATE income_statement SET
    status = CASE
        WHEN handler_id IS NOT NULL
            THEN 'HANDLED'::income_statement_status
            ELSE 'SENT'::income_statement_status
        END,
    sent_at = created;

ALTER TABLE income_statement ALTER COLUMN status SET NOT NULL;

ALTER TABLE income_statement
    ADD CONSTRAINT income_statement_status_check CHECK (
        (status = 'HANDLED'::income_statement_status) = (handler_id IS NOT NULL)
        AND (status = 'DRAFT'::income_statement_status) = (sent_at IS NULL)
    );

CREATE INDEX idx$income_statement_sent_at_awaiting_handler
    ON income_statement (sent_at)
    WHERE (status = 'SENT'::income_statement_status);

DROP INDEX idx$income_statement_created_handler_id_null;
DROP INDEX idx$income_statement_start_date_handler_id_null;
