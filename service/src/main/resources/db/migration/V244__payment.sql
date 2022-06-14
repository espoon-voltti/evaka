CREATE TYPE payment_status AS ENUM ('DRAFT', 'SENT');

CREATE TABLE payment (
    id uuid PRIMARY KEY NOT NULL DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone NOT NULL DEFAULT now(),
    updated timestamp with time zone NOT NULL DEFAULT now(),
    unit_id uuid NOT NULL CONSTRAINT fk$unit REFERENCES daycare (id),
    period daterange NOT NULL,
    number bigint,
    amount int NOT NULL,
    status payment_status NOT NULL,
    payment_date date,
    due_date date,
    sent_at timestamp with time zone,
    sent_by uuid CONSTRAINT fk$sent_by REFERENCES evaka_user (id),

    CONSTRAINT payment_state CHECK (
        status = 'DRAFT' OR (
            payment_date IS NOT NULL AND
            due_date IS NOT NULL AND
            sent_at IS NOT NULL AND
            sent_by IS NOT NULL
        )
    )
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON payment FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();
