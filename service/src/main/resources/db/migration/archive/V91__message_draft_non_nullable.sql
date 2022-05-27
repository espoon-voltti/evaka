ALTER TABLE message_draft RENAME COLUMN recipient_account_ids TO recipient_ids;
UPDATE message_draft SET recipient_ids = '{}' WHERE recipient_ids IS NULL;
ALTER TABLE message_draft ALTER COLUMN recipient_ids SET DEFAULT '{}';
ALTER TABLE message_draft ALTER COLUMN recipient_ids SET NOT NULL;

UPDATE message_draft SET recipient_names = '{}' WHERE recipient_names IS NULL;
ALTER TABLE message_draft ALTER COLUMN recipient_names SET DEFAULT '{}';
ALTER TABLE message_draft ALTER COLUMN recipient_names SET NOT NULL;

UPDATE message_draft SET title = '' WHERE title IS NULL;
ALTER TABLE message_draft ALTER COLUMN title SET DEFAULT '';
ALTER TABLE message_draft ALTER COLUMN title SET NOT NULL;

UPDATE message_draft SET content = '' WHERE content IS NULL;
ALTER TABLE message_draft ALTER COLUMN content SET DEFAULT '';
ALTER TABLE message_draft ALTER COLUMN content SET NOT NULL;

UPDATE message_draft SET type = 'MESSAGE' WHERE type IS NULL;
ALTER TABLE message_draft ALTER COLUMN type SET DEFAULT 'MESSAGE';
ALTER TABLE message_draft ALTER COLUMN type SET NOT NULL;
