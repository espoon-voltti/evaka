ALTER TABLE application
    ADD COLUMN process_id uuid REFERENCES archived_process (id),
    ADD COLUMN created_by uuid REFERENCES evaka_user (id);

-- migrate created_by data where possible (i.e. guardian has sent the application themselves)
UPDATE application a
SET created_by = updates.created_by
FROM (
    SELECT a.id, eu.id AS created_by
    FROM application a
    LEFT JOIN evaka_user eu ON eu.citizen_id = a.guardian_id
    WHERE a.origin = 'ELECTRONIC'
) AS updates
WHERE a.id = updates.id;

-- add missing foreign key indexes
CREATE INDEX fk$child_document_created_by ON child_document(created_by);
CREATE INDEX fk$assistance_need_decision_process_id ON assistance_need_decision(process_id);
CREATE INDEX fk$assistance_need_decision_created_by ON assistance_need_decision(created_by);
CREATE INDEX fk$assistance_need_preschool_decision_process_id ON assistance_need_preschool_decision(process_id);
CREATE INDEX fk$assistance_need_preschool_decision_created_by ON assistance_need_preschool_decision(created_by);
CREATE INDEX fk$application_process_id ON application(process_id);
CREATE INDEX fk$application_created_by ON application(created_by);
