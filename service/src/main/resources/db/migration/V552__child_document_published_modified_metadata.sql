ALTER TABLE child_document RENAME COLUMN content_modified_at TO content_locked_at;
ALTER TABLE child_document RENAME COLUMN content_modified_by TO content_locked_by;

ALTER TABLE child_document
    DROP CONSTRAINT child_document_content_modified_by_fkey,
    ADD CONSTRAINT child_document_content_locked_by_fkey FOREIGN KEY (content_locked_by) REFERENCES evaka_user (id);

DROP INDEX fk$child_document_content_modified_by;
CREATE INDEX fk$child_document_content_locked_by ON child_document (content_locked_by);

ALTER TABLE child_document ADD COLUMN published_by UUID REFERENCES evaka_user(id);

UPDATE child_document
SET published_by = '00000000-0000-0000-0000-000000000000'::UUID
WHERE published_at IS NOT NULL;

ALTER TABLE child_document
    DROP CONSTRAINT published_consistency,
    ADD CONSTRAINT published_consistency CHECK ((published_at IS NULL) = (published_content IS NULL) AND (published_at IS NULL) = (published_by IS NULL));

ALTER TABLE child_document ADD COLUMN modified_by UUID REFERENCES evaka_user(id) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000'::UUID;

CREATE INDEX fk$child_document_published_by ON child_document (published_by);
CREATE INDEX fk$child_document_modified_by ON child_document (modified_by);