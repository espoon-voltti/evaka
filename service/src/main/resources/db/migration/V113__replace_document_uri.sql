ALTER TABLE decision ADD COLUMN document_key text;
ALTER TABLE decision ADD COLUMN other_guardian_document_key text;

UPDATE decision
SET document_key = regexp_replace(document_uri, '[a-z0-9]+://[^/]+/', '');
UPDATE decision
SET other_guardian_document_key = regexp_replace(other_guardian_document_uri, '[a-z0-9]+://[^/]+/', '');

-- double-check all document URIs were converted
DO $$
DECLARE
    problem_id RECORD;
BEGIN
    FOR problem_id IN
        SELECT id
        FROM decision
        WHERE (document_key IS NULL) IS DISTINCT FROM (document_uri IS NULL)
        OR (other_guardian_document_key IS NULL) IS DISTINCT FROM (other_guardian_document_uri IS NULL)
    LOOP
        RAISE EXCEPTION 'Failed to replace document URI for decision %', problem_id;
    END LOOP;
END $$;

ALTER TABLE decision DROP COLUMN document_uri;
ALTER TABLE decision DROP COLUMN other_guardian_document_uri;
