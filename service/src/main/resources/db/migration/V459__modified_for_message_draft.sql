DROP TRIGGER IF EXISTS set_timestamp ON message_draft;

ALTER TABLE message_draft
    RENAME COLUMN updated TO updated_at;

ALTER TABLE message_draft
    RENAME COLUMN created TO created_at;

ALTER TABLE message_draft
    ADD COLUMN modified_at TIMESTAMP WITH TIME ZONE;

UPDATE message_draft
SET modified_at = updated_at;

ALTER TABLE message_draft
    ALTER COLUMN modified_at SET NOT NULL;

CREATE TRIGGER set_timestamp
    BEFORE UPDATE
    ON message_draft
    FOR EACH ROW
EXECUTE PROCEDURE trigger_refresh_updated_at();
