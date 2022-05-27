CREATE TABLE invoice_correction (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    head_of_family_id uuid NOT NULL REFERENCES person(id),
    child_id uuid NOT NULL REFERENCES person(id),
    unit_id uuid NOT NULL REFERENCES daycare(id),
    product text NOT NULL,
    period daterange NOT NULL,
    amount int NOT NULL,
    unit_price int NOT NULL,
    description text NOT NULL,
    note text NOT NULL
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON invoice_correction FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

CREATE INDEX idx$invoice_correction_head_of_family ON invoice_correction (head_of_family_id);
