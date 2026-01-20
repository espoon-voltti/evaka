ALTER TABLE message DROP CONSTRAINT message_replies_to_fkey;
ALTER TABLE message DROP COLUMN replies_to;
