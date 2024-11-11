CREATE TABLE citizen_user (
    id uuid PRIMARY KEY,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    last_strong_login timestamp with time zone,
    last_weak_login timestamp with time zone,
    username text,
    username_updated_at timestamp with time zone,
    password jsonb,
    password_updated_at timestamp with time zone
);

ALTER TABLE citizen_user
    ADD CONSTRAINT fk$person FOREIGN KEY (id) REFERENCES person (id),
    ADD CONSTRAINT uniq$citizen_user_username UNIQUE (username),
    ADD CONSTRAINT check$username_format CHECK (lower(trim(username)) = username), -- lowercase, no extra whitespace around it
    ADD CONSTRAINT check$weak_credentials CHECK (num_nonnulls(username, username_updated_at, password, password_updated_at) = ANY(array[0, 4]));

CREATE TRIGGER set_timestamp BEFORE UPDATE ON citizen_user
    FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();

INSERT INTO citizen_user (id, created_at)
SELECT id, created
FROM person WHERE last_login IS NOT NULL;
