CREATE TABLE income_cta_dismissal (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    person_id uuid NOT NULL,
    income_expiration_date date,
    income_notification_id uuid,
    CONSTRAINT fk$person FOREIGN KEY (person_id) REFERENCES person (id) ON DELETE CASCADE,
    CONSTRAINT fk$income_notification FOREIGN KEY (income_notification_id) REFERENCES income_notification (id) ON DELETE CASCADE,
    CONSTRAINT check$exactly_one_target CHECK (num_nonnulls(income_expiration_date, income_notification_id) = 1),
    CONSTRAINT uniq$income_cta_dismissal_expiration UNIQUE (person_id, income_expiration_date),
    CONSTRAINT uniq$income_cta_dismissal_notification UNIQUE (income_notification_id)
);
