ALTER TABLE pedagogical_document ADD COLUMN modified_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE pedagogical_document RENAME COLUMN updated_by TO modified_by;
ALTER TABLE pedagogical_document RENAME CONSTRAINT fk$updated_by TO fk$modified_by;

UPDATE pedagogical_document SET
    created_by = COALESCE(created_by, '00000000-0000-0000-0000-000000000000'::UUID),
    modified_at = COALESCE(updated, created),
    modified_by = COALESCE(modified_by, created_by, '00000000-0000-0000-0000-000000000000'::UUID);

ALTER TABLE pedagogical_document
    ALTER COLUMN created_by SET NOT NULL,
    ALTER COLUMN modified_by SET NOT NULL,
    ALTER COLUMN modified_at SET NOT NULL;

DROP TRIGGER set_timestamp ON pedagogical_document;
ALTER TABLE pedagogical_document RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON pedagogical_document FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();

ALTER TABLE pedagogical_document RENAME COLUMN created TO created_at;
