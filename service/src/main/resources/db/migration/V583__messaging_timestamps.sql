-- message_account
ALTER TABLE message_account RENAME COLUMN created TO created_at;
ALTER TABLE message_account ADD COLUMN created timestamptz GENERATED ALWAYS AS (created_at) STORED;

DROP TRIGGER IF EXISTS set_timestamp ON message_account;
ALTER TABLE message_account RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON message_account FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
ALTER TABLE message_account ADD COLUMN updated timestamptz GENERATED ALWAYS AS (updated_at) STORED;

-- message_content
ALTER TABLE message_content RENAME COLUMN created TO created_at;
ALTER TABLE message_content ADD COLUMN created timestamptz GENERATED ALWAYS AS (created_at) STORED;

DROP TRIGGER IF EXISTS set_timestamp ON message_content;
ALTER TABLE message_content RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON message_content FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
ALTER TABLE message_content ADD COLUMN updated timestamptz GENERATED ALWAYS AS (updated_at) STORED;

-- message_thread
ALTER TABLE message_thread RENAME COLUMN created TO created_at;
ALTER TABLE message_thread ADD COLUMN created timestamptz GENERATED ALWAYS AS (created_at) STORED;

DROP TRIGGER IF EXISTS set_timestamp ON message_thread;
ALTER TABLE message_thread RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON message_thread FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
ALTER TABLE message_thread ADD COLUMN updated timestamptz GENERATED ALWAYS AS (updated_at) STORED;

-- message
ALTER TABLE message RENAME COLUMN created TO created_at;
ALTER TABLE message ADD COLUMN created timestamptz GENERATED ALWAYS AS (created_at) STORED;

DROP TRIGGER IF EXISTS set_timestamp ON message;
ALTER TABLE message RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON message FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
ALTER TABLE message ADD COLUMN updated timestamptz GENERATED ALWAYS AS (updated_at) STORED;

-- message_recipients
ALTER TABLE message_recipients RENAME COLUMN created TO created_at;
ALTER TABLE message_recipients ADD COLUMN created timestamptz GENERATED ALWAYS AS (created_at) STORED;

DROP TRIGGER IF EXISTS set_timestamp ON message_recipients;
ALTER TABLE message_recipients RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON message_recipients FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
ALTER TABLE message_recipients ADD COLUMN updated timestamptz GENERATED ALWAYS AS (updated_at) STORED;

-- message_thread_children
ALTER TABLE message_thread_children RENAME COLUMN created TO created_at;
ALTER TABLE message_thread_children ADD COLUMN created timestamptz GENERATED ALWAYS AS (created_at) STORED;

DROP TRIGGER IF EXISTS set_timestamp ON message_thread_children;
ALTER TABLE message_thread_children RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON message_thread_children FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
ALTER TABLE message_thread_children ADD COLUMN updated timestamptz GENERATED ALWAYS AS (updated_at) STORED;

-- message_thread_folder
ALTER TABLE message_thread_folder RENAME COLUMN created TO created_at;
ALTER TABLE message_thread_folder ADD COLUMN created timestamptz GENERATED ALWAYS AS (created_at) STORED;

DROP TRIGGER IF EXISTS set_timestamp ON message_thread_folder;
ALTER TABLE message_thread_folder RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON message_thread_folder FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
ALTER TABLE message_thread_folder ADD COLUMN updated timestamptz GENERATED ALWAYS AS (updated_at) STORED;

-- message_thread_participant
ALTER TABLE message_thread_participant RENAME COLUMN created TO created_at;
ALTER TABLE message_thread_participant ADD COLUMN created timestamptz GENERATED ALWAYS AS (created_at) STORED;

DROP TRIGGER IF EXISTS set_timestamp ON message_thread_participant;
ALTER TABLE message_thread_participant RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON message_thread_participant FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
ALTER TABLE message_thread_participant ADD COLUMN updated timestamptz GENERATED ALWAYS AS (updated_at) STORED;
