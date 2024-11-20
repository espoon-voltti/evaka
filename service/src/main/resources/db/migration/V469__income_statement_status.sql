CREATE TYPE income_statement_status AS ENUM ('DRAFT', 'SENT', 'HANDLED');

ALTER TABLE income_statement RENAME COLUMN created to created_at;

DROP TRIGGER set_timestamp ON income_statement;
ALTER TABLE income_statement RENAME COLUMN updated to updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON income_statement
    FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();

ALTER TABLE income_statement
    ADD COLUMN status income_statement_status,
    -- created_at exists
    ADD COLUMN created_by uuid REFERENCES evaka_user,
    ADD COLUMN modified_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN modified_by uuid REFERENCES evaka_user,
    ADD COLUMN sent_at TIMESTAMP WITH TIME ZONE,
    -- sent_by = created_by
    ADD COLUMN handled_at TIMESTAMP WITH TIME ZONE
    -- handled_by = handler_id
;

UPDATE income_statement SET
    status = CASE
        WHEN handler_id IS NOT NULL
            THEN 'HANDLED'::income_statement_status
            ELSE 'SENT'::income_statement_status
        END,
    -- person_id may also refer to child so created_by/modified_by is not reliably known
    created_by = '00000000-0000-0000-0000-000000000000'::UUID,
    modified_by = '00000000-0000-0000-0000-000000000000'::UUID,
    modified_at = updated_at,
    sent_at = created_at,
    handled_at = updated_at -- best guess
;

ALTER TABLE income_statement
    ALTER COLUMN status SET NOT NULL,
    ALTER COLUMN created_by SET NOT NULL,
    ALTER COLUMN modified_at SET NOT NULL,
    ALTER COLUMN modified_by SET NOT NULL;

ALTER TABLE income_statement
    ADD CONSTRAINT income_statement_status_check CHECK (
        (status = 'HANDLED'::income_statement_status) = (handler_id IS NOT NULL) AND
        (status = 'HANDLED'::income_statement_status) = (handled_at IS NOT NULL) AND
        (status = 'DRAFT'::income_statement_status) = (sent_at IS NULL)
    );

DROP INDEX idx$income_statement_created_handler_id_null;
CREATE INDEX idx$income_statement_sent_at_awaiting_handler
    ON income_statement (sent_at)
    WHERE (status = 'SENT'::income_statement_status);

CREATE INDEX idx$income_statement_created_by ON income_statement (created_by);
CREATE INDEX idx$income_statement_modified_by ON income_statement (modified_by);

-- index that appears to be unused
DROP INDEX idx$income_statement_start_date_handler_id_null;
