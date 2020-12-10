ALTER TABLE employee ADD COLUMN external_id text CONSTRAINT uniq$employee_external_id UNIQUE;
UPDATE employee SET external_id = 'espoo-ad:' || aad_object_id::text WHERE aad_object_id IS NOT NULL;
ALTER TABLE employee DROP COLUMN aad_object_id;
