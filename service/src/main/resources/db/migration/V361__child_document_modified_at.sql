ALTER TABLE child_document ADD COLUMN modified_at timestamp with time zone;

UPDATE child_document SET modified_at = updated;

ALTER TABLE child_document ALTER COLUMN modified_at SET NOT NULL;
