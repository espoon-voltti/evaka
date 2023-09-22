ALTER TABLE message_recipients RENAME COLUMN notification_sent_at TO email_notification_sent_at;
ALTER TABLE message_recipients ADD COLUMN push_notification_sent_at timestamp with time zone;
