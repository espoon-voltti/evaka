ALTER TABLE person ADD COLUMN verified_email text;

CREATE TABLE person_email_verification (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    person_id uuid NOT NULL,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    email text NOT NULL,
    verification_code text NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    sent_at timestamp with time zone
);

ALTER TABLE person_email_verification
    ADD CONSTRAINT fk$person FOREIGN KEY (person_id) REFERENCES person (id) ON DELETE CASCADE,
    ADD CONSTRAINT uniq$person_email_verification_person_email UNIQUE (person_id, email)
;
