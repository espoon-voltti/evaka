ALTER TABLE mobile_device
    ADD COLUMN last_seen timestamp with time zone,
    ADD COLUMN user_agent text;
