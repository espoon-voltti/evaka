ALTER TABLE child_document ADD COLUMN content_modified_at timestamp with time zone;
UPDATE child_document SET content_modified_at = modified_at;
ALTER TABLE child_document ALTER COLUMN content_modified_at SET NOT NULL;

ALTER TABLE child_document ADD COLUMN content_modified_by uuid REFERENCES employee(id);
