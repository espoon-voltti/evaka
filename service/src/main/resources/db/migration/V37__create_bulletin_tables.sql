CREATE TABLE bulletin (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    created_by_employee uuid NOT NULL REFERENCES employee (id),
    group_id uuid REFERENCES daycare_group (id) DEFAULT NULL,
    sent_at timestamp with time zone DEFAULT NULL,
    title text NOT NULL DEFAULT '',
    content text NOT NULL DEFAULT ''

    CONSTRAINT group_set_when_sent CHECK ( sent_at IS NULL OR group_id IS NOT NULL )
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON bulletin FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

CREATE INDEX idx$bulletin_created_by_employee ON bulletin (created_by_employee);

CREATE INDEX idx$bulletin_group_id ON bulletin (group_id);

CREATE TABLE bulletin_instance (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    bulletin_id uuid NOT NULL REFERENCES bulletin (id),
    guardian_id uuid NOT NULL REFERENCES person (id),
    read_at timestamp with time zone DEFAULT NULL
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON bulletin_instance FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

CREATE INDEX idx$bulletin_instance_bulletin_id ON bulletin_instance (bulletin_id);

CREATE INDEX idx$bulletin_instance_to_guardian ON bulletin_instance (guardian_id);
