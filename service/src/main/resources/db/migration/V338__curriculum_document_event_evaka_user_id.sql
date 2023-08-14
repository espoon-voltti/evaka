ALTER TABLE curriculum_document_event ADD COLUMN created_by uuid;
CREATE INDEX idx$curriculum_document_event_created_by ON curriculum_document_event (created_by);
ALTER TABLE curriculum_document_event ADD CONSTRAINT fk$created_by FOREIGN KEY (created_by) REFERENCES evaka_user (id);

UPDATE curriculum_document_event SET created_by = evaka_user.id
FROM evaka_user WHERE evaka_user.employee_id = curriculum_document_event.employee_id;

ALTER TABLE curriculum_document_event
    ALTER COLUMN created_by SET NOT NULL,
    DROP COLUMN employee_id;

