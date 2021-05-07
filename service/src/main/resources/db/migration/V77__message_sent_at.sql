ALTER TABLE message ADD COLUMN sent_at timestamp with time zone DEFAULT now() NOT NULL;
CREATE INDEX idx$message_sent_at ON message (sent_at);
DROP INDEX idx$message_created;
