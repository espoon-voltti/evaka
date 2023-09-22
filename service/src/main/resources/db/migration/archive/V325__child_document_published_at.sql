ALTER TABLE child_document ADD COLUMN published_at timestamp with time zone;

UPDATE child_document
SET published_at = updated
WHERE published;

ALTER TABLE child_document DROP COLUMN published;
