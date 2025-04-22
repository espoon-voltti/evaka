ALTER TABLE document_template
    ADD COLUMN archive_externally boolean DEFAULT FALSE NOT NULL;

-- Set existing document types that previously had hard-coded support for archiving to true
UPDATE document_template
SET archive_externally = TRUE
WHERE type IN ('VASU', 'LEOPS', 'HOJKS', 'MIGRATED_VASU', 'MIGRATED_LEOPS');