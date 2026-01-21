CREATE TABLE child_document_pdf_version (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    child_document_id uuid NOT NULL REFERENCES child_document(id) ON DELETE CASCADE,
    document_key text,
    created_at timestamp with time zone NOT NULL,
    version_number integer NOT NULL,
    CONSTRAINT unique_version_per_document UNIQUE(child_document_id, version_number)
);

CREATE INDEX fk$child_document_pdf_version_document_id
    ON child_document_pdf_version(child_document_id);

INSERT INTO child_document_pdf_version (child_document_id, document_key, created_at, version_number)
SELECT
    id AS child_document_id,
    document_key,
    COALESCE(published_at, modified_at, created) AS created_at,
    1 AS version_number
FROM child_document
WHERE document_key IS NOT NULL;
