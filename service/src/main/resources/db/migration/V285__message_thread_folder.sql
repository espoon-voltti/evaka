CREATE TABLE message_thread_folder
(
    id                      uuid PRIMARY KEY         NOT NULL DEFAULT ext.uuid_generate_v1mc(),
    created                 timestamp with time zone NOT NULL DEFAULT now(),
    updated                 timestamp with time zone NOT NULL DEFAULT now(),
    name                    text                     NOT NULL DEFAULT '',
    owner_id                uuid                     NOT NULL REFERENCES message_account (id) ON DELETE CASCADE,

    CONSTRAINT uniq$thread_folder UNIQUE (owner_id, name)
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON message_thread_folder FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

ALTER TABLE message_thread_participant
    ADD COLUMN folder_id uuid REFERENCES message_thread_folder (id) ON DELETE CASCADE;

DROP INDEX idx$thread_participant_message;
CREATE INDEX idx$thread_participant_message
    ON message_thread_participant (participant_id, folder_id, last_message_timestamp);

DROP INDEX idx$thread_participant_received;
CREATE INDEX idx$thread_participant_received
    ON message_thread_participant (participant_id, folder_id, last_received_timestamp)
    WHERE (last_received_timestamp IS NOT NULL);
