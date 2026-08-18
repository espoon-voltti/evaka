CREATE TABLE citizen_passkey (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    citizen_user_id uuid NOT NULL,
    credential_id bytea NOT NULL,
    public_key bytea NOT NULL,
    signature_counter bigint NOT NULL,
    aaguid uuid NOT NULL,
    transports text[] NOT NULL,
    name text NOT NULL,
    device_class text NOT NULL,
    operating_system_name text NOT NULL,
    agent_name text NOT NULL,
    last_used_at timestamp with time zone
);

ALTER TABLE citizen_passkey
    ADD CONSTRAINT fk$citizen_user FOREIGN KEY (citizen_user_id) REFERENCES citizen_user (id) ON DELETE CASCADE,
    ADD CONSTRAINT uniq$citizen_passkey_credential_id UNIQUE (credential_id);

CREATE INDEX idx$citizen_passkey_citizen_user_id ON citizen_passkey (citizen_user_id);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON citizen_passkey
    FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();

CREATE TABLE citizen_passkey_registration (
    person_id uuid PRIMARY KEY,
    options jsonb NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    created_at timestamp with time zone NOT NULL DEFAULT now()
);

ALTER TABLE citizen_passkey_registration
    ADD CONSTRAINT fk$person FOREIGN KEY (person_id) REFERENCES person (id) ON DELETE CASCADE;
