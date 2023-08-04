CREATE TABLE vapid_jwt (
    origin text NOT NULL,
    public_key bytea NOT NULL,
    created timestamp with time zone NOT NULL DEFAULT now(),
    updated timestamp with time zone NOT NULL DEFAULT now(),

    jwt text NOT NULL,
    expires_at timestamp with time zone NOT NULL,

    PRIMARY KEY (origin, public_key)
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON vapid_jwt FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();
