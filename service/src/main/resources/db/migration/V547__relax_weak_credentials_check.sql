ALTER TABLE citizen_user
    DROP CONSTRAINT check$weak_credentials,
    ADD CONSTRAINT check$weak_credentials CHECK ((username IS NULL) = (password IS NULL)),
    ADD CONSTRAINT check$weak_credentials_updated CHECK (
        (username IS NULL OR username_updated_at IS NOT NULL) AND
        (username_updated_at IS NULL) = (password_updated_at IS NULL)
    );
