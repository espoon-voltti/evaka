INSERT INTO application_other_guardian (application_id, guardian_id)
SELECT id, other_guardian_id
FROM application
WHERE other_guardian_id IS NOT NULL
ON CONFLICT (application_id, guardian_id) DO NOTHING;

ALTER TABLE application DROP COLUMN other_guardian_id;
