ALTER TABLE message
    ADD COLUMN content_deleted_at timestamp with time zone,
    ADD COLUMN content_deleted_by_employee_id uuid REFERENCES employee (id);

ALTER TABLE message
    ADD CONSTRAINT message_content_deleted_consistency
        CHECK ((content_deleted_at IS NULL) = (content_deleted_by_employee_id IS NULL));

CREATE INDEX idx$message_content_deleted_at ON message (content_deleted_at)
    WHERE content_deleted_at IS NOT NULL;

CREATE INDEX idx$message_content_deleted_by_employee_id ON message (content_deleted_by_employee_id)
    WHERE content_deleted_by_employee_id IS NOT NULL;
