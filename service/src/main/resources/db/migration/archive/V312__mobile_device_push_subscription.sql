CREATE TABLE mobile_device_push_subscription (
    device uuid NOT NULL CONSTRAINT fk$mobile_device REFERENCES mobile_device (id) ON DELETE CASCADE,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    endpoint text NOT NULL,
    expires timestamp with time zone,
    auth_secret bytea NOT NULL,
    ecdh_key bytea NOT NULL,

    PRIMARY KEY (device)
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON mobile_device_push_subscription FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();
