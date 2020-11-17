CREATE TABLE attachment (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    name text NOT NULL,
    content_type text NOT NULL,
    application_id uuid REFERENCES application(id)
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON attachment FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();
