ALTER TYPE document_template_type ADD VALUE 'HOJKS';

CREATE TYPE child_document_status AS ENUM (
    'DRAFT',
    'PREPARED',
    'COMPLETED'
);

ALTER TABLE child_document
    ADD COLUMN status child_document_status NOT NULL DEFAULT 'DRAFT',
    ADD COLUMN published_content jsonb DEFAULT NULL;

ALTER TABLE child_document ALTER COLUMN status DROP DEFAULT;

UPDATE child_document
SET status = 'COMPLETED', published_content = content
WHERE published_at IS NOT NULL;

ALTER TABLE child_document
    ADD CONSTRAINT published_consistency CHECK ( (published_at IS NULL) = (published_content IS NULL) ),
    ADD CONSTRAINT non_drafts_are_published CHECK ( status = 'DRAFT' OR published_at IS NOT NULL );
