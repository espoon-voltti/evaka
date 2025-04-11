ALTER TABLE child_document ADD COLUMN type document_template_type;

UPDATE child_document
SET type = dt.type
FROM document_template dt
WHERE dt.id = child_document.template_id;

ALTER TABLE child_document ALTER COLUMN type SET NOT NULL;

ALTER TABLE child_document
    ADD CONSTRAINT valid_status CHECK (
        CASE
            WHEN type = 'PEDAGOGICAL_REPORT'
                THEN status IN ('DRAFT', 'COMPLETED')
            WHEN type = 'PEDAGOGICAL_ASSESSMENT'
                THEN status IN ('DRAFT', 'COMPLETED')
            WHEN type = 'HOJKS'
                THEN status IN ('DRAFT', 'PREPARED', 'COMPLETED')
            WHEN type = 'MIGRATED_VASU'
                THEN status IN ('COMPLETED')
            WHEN type = 'MIGRATED_LEOPS'
                THEN status IN ('COMPLETED')
            WHEN type = 'VASU'
                THEN status IN ('DRAFT', 'PREPARED', 'COMPLETED')
            WHEN type = 'LEOPS'
                THEN status IN ('DRAFT', 'PREPARED', 'COMPLETED')
            WHEN type = 'CITIZEN_BASIC'
                THEN status IN ('DRAFT', 'CITIZEN_DRAFT', 'COMPLETED')
            WHEN type = 'OTHER'
                THEN status IN ('DRAFT', 'COMPLETED')
        END
    );

ALTER TABLE child_document
    ADD CONSTRAINT answerable_document CHECK (
        type = 'CITIZEN_BASIC' OR (answered_at IS NULL AND answered_by IS NULL)
    );
