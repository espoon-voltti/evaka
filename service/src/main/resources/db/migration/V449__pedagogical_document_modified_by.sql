ALTER TABLE pedagogical_document ADD COLUMN modified_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE pedagogical_document RENAME COLUMN updated_by TO modified_by;
ALTER TABLE pedagogical_document RENAME CONSTRAINT fk$updated_by TO fk$modified_by;

UPDATE pedagogical_document SET
    modified_at = COALESCE(updated, created),
    modified_by = COALESCE(modified_by, created_by);

