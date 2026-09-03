ALTER TABLE person
    ADD COLUMN disabled_push_types notification_category[] NOT NULL DEFAULT '{}';

CREATE TABLE citizen_push_subscription (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    person_id uuid NOT NULL,
    endpoint text NOT NULL,
    auth_secret bytea NOT NULL,
    ecdh_key bytea NOT NULL,
    expires_at timestamp with time zone,
    installed boolean NOT NULL,
    device_class text NOT NULL,
    operating_system_name text NOT NULL,
    agent_name text NOT NULL,
    last_sent_at timestamp with time zone
);

ALTER TABLE citizen_push_subscription
    ADD CONSTRAINT fk$person FOREIGN KEY (person_id) REFERENCES person (id) ON DELETE CASCADE,
    ADD CONSTRAINT uniq$citizen_push_subscription_endpoint UNIQUE (endpoint);

CREATE INDEX idx$citizen_push_subscription_person_id ON citizen_push_subscription (person_id);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON citizen_push_subscription
    FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
