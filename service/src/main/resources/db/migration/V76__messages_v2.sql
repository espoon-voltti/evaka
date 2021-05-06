DROP TABLE message_thread_messages;
DROP TABLE message_participants;

CREATE TABLE message_content (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    author_id uuid references message_account(id) NOT NULL,
    content text NOT NULL
);
CREATE TRIGGER set_timestamp BEFORE UPDATE ON message_content FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

CREATE TABLE message_recipients (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    message_id uuid references message(id) NOT NULL,
    recipient_id uuid references message_account(id) NOT NULL,
    read_at timestamp with time zone
);
CREATE TRIGGER set_timestamp BEFORE UPDATE ON message_recipients FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();
CREATE INDEX idx$message_recipients_message_id ON message_recipients (message_id);
CREATE INDEX idx$message_recipients_account_id ON message_recipients (recipient_id);

ALTER TABLE message DROP COLUMN title;
ALTER TABLE message_thread ADD COLUMN title text NOT NULL;

ALTER TABLE message DROP COLUMN content;
ALTER TABLE message ADD COLUMN content_id uuid NOT NULL references message_content(id);
CREATE INDEX idx$message_content_id ON message (content_id);

ALTER TABLE message ADD COLUMN replies_to uuid references message(id);
ALTER TABLE message ADD COLUMN thread_id uuid NOT NULL references message_thread(id);
ALTER TABLE message ADD COLUMN sender_id uuid NOT NULL references message_account(id);
CREATE INDEX idx$message_thread_id ON message (thread_id);
CREATE INDEX idx$message_sender_id ON message (sender_id);
