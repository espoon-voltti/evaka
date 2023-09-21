CREATE TABLE message_thread_participant
(
    id                      uuid PRIMARY KEY         NOT NULL DEFAULT ext.uuid_generate_v1mc(),
    created                 timestamp with time zone NOT NULL DEFAULT now(),
    updated                 timestamp with time zone NOT NULL DEFAULT now(),
    thread_id               uuid                     NOT NULL REFERENCES message_thread (id) ON DELETE CASCADE,
    participant_id          uuid                     NOT NULL REFERENCES message_account (id) ON DELETE CASCADE,
    last_message_timestamp  timestamp with time zone NOT NULL,  -- timestamp of last sent or received message
    last_received_timestamp timestamp with time zone,           -- timestamp of last received message
    last_sent_timestamp     timestamp with time zone,           -- timestamp of last sent message

    CONSTRAINT uniq$thread_participant UNIQUE (thread_id, participant_id)
);

CREATE INDEX idx$thread_participant_message ON message_thread_participant (participant_id, last_message_timestamp);
CREATE INDEX idx$thread_participant_received ON message_thread_participant (participant_id, last_received_timestamp) WHERE last_received_timestamp IS NOT NULL;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON message_thread_participant FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();
