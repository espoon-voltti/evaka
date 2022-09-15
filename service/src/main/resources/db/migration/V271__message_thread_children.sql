CREATE TABLE message_thread_children
(
    id         uuid PRIMARY KEY         DEFAULT ext.uuid_generate_v1mc(),
    created    timestamp with time zone DEFAULT now() NOT NULL,
    updated    timestamp with time zone DEFAULT now() NOT NULL,
    thread_id  uuid references message_thread (id)    NOT NULL,
    child_id   uuid references child (id)             NOT NULL
);
CREATE TRIGGER set_timestamp
    BEFORE UPDATE
    ON message_thread_children
    FOR EACH ROW
EXECUTE PROCEDURE trigger_refresh_updated();
CREATE INDEX idx$message_thread_children_thread_id ON message_thread_children (thread_id);
CREATE INDEX idx$message_thread_children_account_id ON message_thread_children (child_id);
