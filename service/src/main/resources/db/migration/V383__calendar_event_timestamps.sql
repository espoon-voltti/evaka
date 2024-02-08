ALTER TABLE calendar_event
    ADD COLUMN modified_at         timestamp with time zone,
    ADD COLUMN content_modified_at timestamp with time zone;

UPDATE calendar_event
SET modified_at = updated, content_modified_at = updated;

ALTER TABLE calendar_event
    ALTER COLUMN modified_at SET NOT NULL,
    ALTER COLUMN content_modified_at SET NOT NULL;

ALTER TABLE calendar_event
    RENAME COLUMN created TO created_at;

DROP TRIGGER set_timestamp ON calendar_event;
ALTER TABLE calendar_event
    RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp
    BEFORE UPDATE
    ON calendar_event
    FOR EACH ROW
EXECUTE PROCEDURE trigger_refresh_updated_at();
