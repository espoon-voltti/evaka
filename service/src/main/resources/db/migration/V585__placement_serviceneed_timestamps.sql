-- placement_plan
ALTER TABLE placement_plan RENAME COLUMN created TO created_at;
ALTER TABLE placement_plan ADD COLUMN created timestamptz GENERATED ALWAYS AS (created_at) STORED;

DROP TRIGGER IF EXISTS set_timestamp ON placement_plan;
ALTER TABLE placement_plan RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON placement_plan FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
ALTER TABLE placement_plan ADD COLUMN updated timestamptz GENERATED ALWAYS AS (updated_at) STORED;

-- service_need
ALTER TABLE service_need RENAME COLUMN created TO created_at;
ALTER TABLE service_need ADD COLUMN created timestamptz GENERATED ALWAYS AS (created_at) STORED;

DROP TRIGGER IF EXISTS set_timestamp ON service_need;
ALTER TABLE service_need RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON service_need FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
ALTER TABLE service_need ADD COLUMN updated timestamptz GENERATED ALWAYS AS (updated_at) STORED;

-- service_need_option
ALTER TABLE service_need_option RENAME COLUMN created TO created_at;
ALTER TABLE service_need_option ADD COLUMN created timestamptz GENERATED ALWAYS AS (created_at) STORED;

DROP TRIGGER IF EXISTS set_timestamp ON service_need_option;
ALTER TABLE service_need_option RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON service_need_option FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
ALTER TABLE service_need_option ADD COLUMN updated timestamptz GENERATED ALWAYS AS (updated_at) STORED;

-- service_need_option_fee
ALTER TABLE service_need_option_fee RENAME COLUMN created TO created_at;
ALTER TABLE service_need_option_fee ADD COLUMN created timestamptz GENERATED ALWAYS AS (created_at) STORED;

DROP TRIGGER IF EXISTS set_timestamp ON service_need_option_fee;
ALTER TABLE service_need_option_fee RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON service_need_option_fee FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
ALTER TABLE service_need_option_fee ADD COLUMN updated timestamptz GENERATED ALWAYS AS (updated_at) STORED;

-- service_need_option_voucher_value
ALTER TABLE service_need_option_voucher_value RENAME COLUMN created TO created_at;
ALTER TABLE service_need_option_voucher_value ADD COLUMN created timestamptz GENERATED ALWAYS AS (created_at) STORED;

DROP TRIGGER IF EXISTS set_timestamp ON service_need_option_voucher_value;
ALTER TABLE service_need_option_voucher_value RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON service_need_option_voucher_value FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
ALTER TABLE service_need_option_voucher_value ADD COLUMN updated timestamptz GENERATED ALWAYS AS (updated_at) STORED;

-- decision
ALTER TABLE decision RENAME COLUMN created TO created_at;
ALTER TABLE decision ADD COLUMN created timestamptz GENERATED ALWAYS AS (created_at) STORED;

DROP TRIGGER IF EXISTS set_timestamp ON decision;
ALTER TABLE decision RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON decision FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
ALTER TABLE decision ADD COLUMN updated timestamptz GENERATED ALWAYS AS (updated_at) STORED;

-- daily_service_time
ALTER TABLE daily_service_time RENAME COLUMN created TO created_at;
ALTER TABLE daily_service_time ADD COLUMN created timestamptz GENERATED ALWAYS AS (created_at) STORED;

DROP TRIGGER IF EXISTS set_timestamp ON daily_service_time;
ALTER TABLE daily_service_time RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON daily_service_time FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
ALTER TABLE daily_service_time ADD COLUMN updated timestamptz GENERATED ALWAYS AS (updated_at) STORED;
