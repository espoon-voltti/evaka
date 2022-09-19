ALTER TABLE message_thread ADD COLUMN is_copy BOOLEAN;
UPDATE message_thread SET is_copy = false;
ALTER TABLE message_thread ALTER COLUMN is_copy SET NOT NULL;
