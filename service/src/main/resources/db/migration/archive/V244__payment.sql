CREATE TYPE payment_status AS ENUM ('DRAFT', 'SENT');

CREATE TABLE payment (
    id uuid PRIMARY KEY NOT NULL DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone NOT NULL DEFAULT now(),
    updated timestamp with time zone NOT NULL DEFAULT now(),
    unit_id uuid NOT NULL,
    unit_name text NOT NULL,
    unit_business_id text,
    unit_iban text,
    unit_provider_id text,
    period daterange NOT NULL,
    number bigint,
    amount int NOT NULL,
    status payment_status NOT NULL,
    payment_date date,
    due_date date,
    sent_at timestamp with time zone,
    sent_by uuid,

    CONSTRAINT fk$payment_unit FOREIGN KEY (unit_id) REFERENCES daycare (id),
    CONSTRAINT unique$payment_number UNIQUE (number),
    CONSTRAINT fk$payment_sent_by FOREIGN KEY (sent_by) REFERENCES evaka_user (id),
    CONSTRAINT check$payment_state CHECK (
        status = 'DRAFT' OR (
            unit_business_id IS NOT NULL AND unit_business_id <> '' AND
            unit_iban IS NOT NULL AND unit_iban <> '' AND
            unit_provider_id IS NOT NULL AND unit_provider_id <> '' AND
            number IS NOT NULL AND
            payment_date IS NOT NULL AND
            due_date IS NOT NULL AND
            sent_at IS NOT NULL AND
            sent_by IS NOT NULL
        )
    )
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON payment FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();
CREATE INDEX idx$payment_created ON payment (created);
CREATE INDEX idx$payment_date ON payment (payment_date);
CREATE INDEX idx$payment_period ON payment USING GIST (period);
CREATE INDEX idx$payment_amount ON payment (amount);
