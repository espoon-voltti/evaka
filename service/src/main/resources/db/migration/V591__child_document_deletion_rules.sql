ALTER TABLE child_document ADD COLUMN status_modified_at timestamp with time zone;
UPDATE child_document SET status_modified_at = modified_at;
ALTER TABLE child_document ALTER COLUMN status_modified_at SET NOT NULL;

CREATE TYPE document_deletion_basis AS ENUM ('PLACEMENT_END', 'STATUS_TRANSITION');

ALTER TABLE document_template
    ADD COLUMN deletion_retention_days int,
    ADD COLUMN deletion_retention_basis document_deletion_basis;

UPDATE document_template
SET deletion_retention_days = 3650,
    deletion_retention_basis = 'PLACEMENT_END';

ALTER TABLE document_template
    ALTER COLUMN deletion_retention_days SET NOT NULL,
    ALTER COLUMN deletion_retention_basis SET NOT NULL;

ALTER TABLE document_template
    ADD CONSTRAINT check$deletion_retention_days_positive
        CHECK (deletion_retention_days > 0);
