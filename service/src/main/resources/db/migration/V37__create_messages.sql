CREATE TABLE message (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    title text NOT NULL,
    content text NOT NULL,
    created_by_employee uuid NOT NULL REFERENCES employee (id)
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON message FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

CREATE INDEX idx$message_created_by_employee ON message (created_by_employee);

CREATE TABLE message_instance (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    message_id uuid NOT NULL REFERENCES message (id),
    guardian_id uuid NOT NULL REFERENCES person (id)
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON message_instance FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

CREATE INDEX idx$message_instance_to_guardian ON message_instance (guardian_id);
