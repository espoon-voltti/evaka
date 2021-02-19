CREATE TYPE message_type AS ENUM (
    'MESSAGE',
    'RELEASE'
);

CREATE TABLE message (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    type message_type NOT NULL,
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
    daycare_id uuid NOT NULL REFERENCES daycare (id),
    group_id uuid NOT NULL REFERENCES daycare_group (id),
    child_id uuid NOT NULL REFERENCES child (id),
    guardian_id uuid NOT NULL REFERENCES person (id)
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON message_instance FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

CREATE INDEX idx$message_instance_to_guardian ON message_instance (guardian_id);

CREATE TABLE message_read (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    message_id uuid NOT NULL REFERENCES message_instance (id),
    read_by uuid NOT NULL REFERENCES person (id),
    read_at timestamp with time zone NOT NULL
);

CREATE UNIQUE INDEX idx$message_read ON message_read (message_id, read_by);
