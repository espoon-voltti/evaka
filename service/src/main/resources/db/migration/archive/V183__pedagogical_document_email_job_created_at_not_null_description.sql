ALTER TABLE pedagogical_document ADD COLUMN email_job_created_at timestamp;

UPDATE pedagogical_document SET description = '' WHERE description IS NULL;
ALTER TABLE pedagogical_document ALTER COLUMN description SET NOT NULL;
