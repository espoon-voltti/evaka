CREATE TABLE message_draft (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    account_id uuid NOT NULL REFERENCES message_account(id) ON DELETE CASCADE,
    recipients uuid[],
    title text,
    content text
);
CREATE TRIGGER set_timestamp BEFORE UPDATE ON message_draft FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

CREATE INDEX idx$message_draft_account_id ON message_draft (account_id);
