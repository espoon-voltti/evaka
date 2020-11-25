CREATE TYPE pairing_status AS ENUM ('WAITING_CHALLENGE', 'WAITING_RESPONSE', 'READY', 'PAIRED');

CREATE TABLE pairing(
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    unit_id uuid NOT NULL REFERENCES daycare(id),
    expires timestamp with time zone NOT NULL,
    status pairing_status NOT NULL DEFAULT 'WAITING_CHALLENGE'::pairing_status,
    challenge_key text NOT NULL,
    response_key text DEFAULT NULL,
    attempts integer NOT NULL DEFAULT 0
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON pairing FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

CREATE UNIQUE INDEX uniq$pairing_challenge_key ON pairing(challenge_key);
CREATE UNIQUE INDEX uniq$pairing_response_key ON pairing(response_key);
