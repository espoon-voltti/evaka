CREATE INDEX idx$message_recipients_unread ON message_recipients (recipient_id, message_id) WHERE read_at IS NULL;
