CREATE TABLE IF NOT EXISTS pedagogical_document (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    child_id uuid REFERENCES person(id) NOT NULL,
    attachment_id uuid REFERENCES attachment(id),
    description text,
    created timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid REFERENCES employee(id),
    updated timestamp with time zone DEFAULT now() NOT NULL,
    updated_by uuid REFERENCES employee(id)
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON pedagogical_document FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();
