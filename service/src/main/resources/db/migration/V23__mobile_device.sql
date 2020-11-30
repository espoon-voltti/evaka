CREATE TABLE mobile_device(
    id uuid PRIMARY KEY REFERENCES employee(id),
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    unit_id uuid NOT NULL REFERENCES daycare(id),
    name text NOT NULL,
    deleted boolean NOT NULL DEFAULT false
);

ALTER TABLE pairing
    ADD COLUMN mobile_device_id uuid REFERENCES mobile_device(id);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON mobile_device FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

CREATE INDEX idx$mobile_device_unit ON mobile_device (unit_id) WHERE deleted IS FALSE;
