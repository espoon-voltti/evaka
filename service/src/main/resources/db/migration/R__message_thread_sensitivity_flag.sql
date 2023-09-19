-- TODO rename this file into V###__message_thread_sensitivity_flag.sql and add to migrations.txt
-- TODO remove this once ready for review
ALTER TABLE message_draft DROP COLUMN IF EXISTS sensitive;
ALTER TABLE message_draft ADD COLUMN sensitive boolean default false not null;
-- TODO remove this once ready for review
ALTER TABLE message_thread DROP COLUMN IF EXISTS sensitive;
ALTER TABLE message_thread ADD COLUMN sensitive boolean default false not null;
ALTER TABLE message_thread ALTER COLUMN sensitive DROP DEFAULT;
