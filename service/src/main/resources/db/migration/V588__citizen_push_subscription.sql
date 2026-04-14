CREATE TABLE citizen_push_subscription (
    citizen_id       uuid        NOT NULL REFERENCES person(id) ON DELETE CASCADE,
    device_id        uuid        NOT NULL,
    expo_push_token  text        NOT NULL,
    created_at       timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (citizen_id, device_id)
);

CREATE INDEX idx_citizen_push_subscription_citizen ON citizen_push_subscription (citizen_id);
