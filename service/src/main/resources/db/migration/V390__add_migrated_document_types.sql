ALTER TYPE document_template_type ADD VALUE 'MIGRATED_VASU';
ALTER TYPE document_template_type ADD VALUE 'MIGRATED_LEOPS';

ALTER TABLE child_document_read
    DROP CONSTRAINT child_document_read_document_id_fkey,
    ADD CONSTRAINT child_document_read_document_id_fkey
        FOREIGN KEY (document_id) REFERENCES child_document ON DELETE CASCADE;
