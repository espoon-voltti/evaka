-- child_daily_note
ALTER TABLE child_daily_note RENAME COLUMN created TO created_at;
-- ALTER TABLE child_daily_note ADD COLUMN created timestamptz GENERATED ALWAYS AS (created_at) STORED;

DROP TRIGGER IF EXISTS set_timestamp ON child_daily_note;
ALTER TABLE child_daily_note RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON child_daily_note FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
-- ALTER TABLE child_daily_note ADD COLUMN updated timestamptz GENERATED ALWAYS AS (updated_at) STORED;

-- child_sticky_note
ALTER TABLE child_sticky_note RENAME COLUMN created TO created_at;
-- ALTER TABLE child_sticky_note ADD COLUMN created timestamptz GENERATED ALWAYS AS (created_at) STORED;

DROP TRIGGER IF EXISTS set_timestamp ON child_sticky_note;
ALTER TABLE child_sticky_note RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON child_sticky_note FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
-- ALTER TABLE child_sticky_note ADD COLUMN updated timestamptz GENERATED ALWAYS AS (updated_at) STORED;

-- group_note
ALTER TABLE group_note RENAME COLUMN created TO created_at;
-- ALTER TABLE group_note ADD COLUMN created timestamptz GENERATED ALWAYS AS (created_at) STORED;

DROP TRIGGER IF EXISTS set_timestamp ON group_note;
ALTER TABLE group_note RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON group_note FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
-- ALTER TABLE group_note ADD COLUMN updated timestamptz GENERATED ALWAYS AS (updated_at) STORED;

-- pairing
ALTER TABLE pairing RENAME COLUMN created TO created_at;
-- ALTER TABLE pairing ADD COLUMN created timestamptz GENERATED ALWAYS AS (created_at) STORED;

DROP TRIGGER IF EXISTS set_timestamp ON pairing;
ALTER TABLE pairing RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON pairing FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
-- ALTER TABLE pairing ADD COLUMN updated timestamptz GENERATED ALWAYS AS (updated_at) STORED;

-- mobile_device
ALTER TABLE mobile_device RENAME COLUMN created TO created_at;
-- ALTER TABLE mobile_device ADD COLUMN created timestamptz GENERATED ALWAYS AS (created_at) STORED;

DROP TRIGGER IF EXISTS set_timestamp ON mobile_device;
ALTER TABLE mobile_device RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON mobile_device FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
-- ALTER TABLE mobile_device ADD COLUMN updated timestamptz GENERATED ALWAYS AS (updated_at) STORED;

-- mobile_device_push_subscription
ALTER TABLE mobile_device_push_subscription RENAME COLUMN created TO created_at;
-- ALTER TABLE mobile_device_push_subscription ADD COLUMN created timestamptz GENERATED ALWAYS AS (created_at) STORED;

DROP TRIGGER IF EXISTS set_timestamp ON mobile_device_push_subscription;
ALTER TABLE mobile_device_push_subscription RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON mobile_device_push_subscription FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
-- ALTER TABLE mobile_device_push_subscription ADD COLUMN updated timestamptz GENERATED ALWAYS AS (updated_at) STORED;

-- vapid_jwt
ALTER TABLE vapid_jwt RENAME COLUMN created TO created_at;
-- ALTER TABLE vapid_jwt ADD COLUMN created timestamptz GENERATED ALWAYS AS (created_at) STORED;

DROP TRIGGER IF EXISTS set_timestamp ON vapid_jwt;
ALTER TABLE vapid_jwt RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON vapid_jwt FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
-- ALTER TABLE vapid_jwt ADD COLUMN updated timestamptz GENERATED ALWAYS AS (updated_at) STORED;
