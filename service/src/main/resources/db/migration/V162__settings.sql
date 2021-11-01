CREATE TYPE setting_type AS ENUM (
    'DECISION_MAKER_NAME',
    'DECISION_MAKER_TITLE'
);

CREATE TABLE setting (
    key setting_type PRIMARY KEY,
    value text NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON setting FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();
