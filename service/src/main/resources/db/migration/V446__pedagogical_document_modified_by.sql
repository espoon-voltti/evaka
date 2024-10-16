ALTER TABLE pedagogical_document
    ADD COLUMN modified_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN modified_by UUID REFERENCES evaka_user;

UPDATE pedagogical_document SET modified_at = created;
UPDATE pedagogical_document SET modified_by = created_by;

UPDATE pedagogical_document SET modified_at = updated;
UPDATE pedagogical_document SET modified_by = updated_by;

ALTER TABLE pedagogical_document ALTER COLUMN modified_at SET NOT NULL;
ALTER TABLE pedagogical_document ALTER COLUMN modified_by SET NOT NULL;

CREATE INDEX fk$pedagogical_document_modified_by ON pedagogical_document(modified_by);
