CREATE TABLE child_document_published_version (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    child_document_id uuid NOT NULL REFERENCES child_document(id),
    document_key text,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    version_number integer NOT NULL,
    created_by uuid NOT NULL REFERENCES evaka_user(id),
    published_content jsonb NOT NULL,
    CONSTRAINT unique_version_per_document UNIQUE(child_document_id, version_number)
);

CREATE INDEX fk$child_document_published_version_document_id_version_number
    ON child_document_published_version(child_document_id, version_number);

CREATE INDEX fk$child_document_published_version_created_by
    ON child_document_published_version(created_by);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON child_document_published_version
    FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();

INSERT INTO child_document_published_version (child_document_id, document_key, created_at, version_number, created_by, published_content)
SELECT
    cd.id AS child_document_id,
    cd.document_key,
    cd.published_at AS created_at,
    1 AS version_number,
    cd.published_by AS created_by,
    cd.published_content
FROM child_document cd
WHERE cd.published_at IS NOT NULL;

ALTER TABLE child_document
    DROP CONSTRAINT published_consistency;

ALTER TABLE child_document
    RENAME COLUMN published_at TO deprecated_published_at;

ALTER TABLE child_document
    RENAME COLUMN published_by TO deprecated_published_by;

ALTER TABLE child_document
    RENAME COLUMN published_content TO deprecated_published_content;

ALTER TABLE child_document
    RENAME COLUMN document_key TO deprecated_document_key;

