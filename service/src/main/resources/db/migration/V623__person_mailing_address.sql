ALTER TABLE person
    ADD COLUMN mailing_street_address text NOT NULL DEFAULT '',
    ADD COLUMN mailing_postal_code text NOT NULL DEFAULT '',
    ADD COLUMN mailing_post_office text NOT NULL DEFAULT '';

CREATE TYPE address_period_type AS ENUM ('MAILING', 'TEMPORARY');

CREATE TABLE person_address_period (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    person_id uuid NOT NULL,
    type address_period_type NOT NULL,
    valid_during daterange NOT NULL CONSTRAINT check$valid_during CHECK (NOT lower_inf(valid_during)),
    street_address text NOT NULL,
    postal_code text NOT NULL,
    post_office text NOT NULL
);

ALTER TABLE person_address_period
    ADD CONSTRAINT fk$person FOREIGN KEY (person_id) REFERENCES person (id) ON DELETE CASCADE;

CREATE INDEX idx$person_address_period_person_id ON person_address_period (person_id);
