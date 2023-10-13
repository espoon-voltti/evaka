ALTER TABLE message_draft ADD COLUMN sensitive boolean default false not null;
ALTER TABLE message_thread ADD COLUMN sensitive boolean default false not null;
ALTER TABLE message_thread ALTER COLUMN sensitive DROP DEFAULT;
