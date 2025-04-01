INSERT INTO evaka_user (id, type, employee_id, name)
SELECT DISTINCT employee.id,
                'EMPLOYEE'::evaka_user_type,
                employee.id,
                last_name || ' ' || coalesce(preferred_first_name, first_name)
FROM child_document
         JOIN employee ON child_document.created_by = employee.id
UNION
SELECT DISTINCT employee.id,
                'EMPLOYEE'::evaka_user_type,
                employee.id,
                last_name || ' ' || coalesce(preferred_first_name, first_name)
FROM child_document
         JOIN employee ON child_document.content_modified_by = employee.id
ON CONFLICT (id) DO NOTHING;

ALTER TABLE child_document
    DROP CONSTRAINT child_document_created_by_fkey;
ALTER TABLE child_document
    ADD CONSTRAINT child_document_created_by_fkey FOREIGN KEY (created_by) REFERENCES evaka_user (id);

ALTER TABLE child_document
    DROP CONSTRAINT child_document_content_modified_by_fkey;
ALTER TABLE child_document
    ADD CONSTRAINT child_document_content_modified_by_fkey FOREIGN KEY (content_modified_by) REFERENCES evaka_user (id);

CREATE INDEX fk$child_document_content_modified_by ON child_document (content_modified_by);
