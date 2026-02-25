-- staff_attendance
ALTER TABLE staff_attendance RENAME COLUMN created TO created_at;
-- ALTER TABLE staff_attendance ADD COLUMN created timestamptz GENERATED ALWAYS AS (created_at) STORED;

DROP TRIGGER IF EXISTS set_timestamp ON staff_attendance;
ALTER TABLE staff_attendance RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON staff_attendance FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
-- ALTER TABLE staff_attendance ADD COLUMN updated timestamptz GENERATED ALWAYS AS (updated_at) STORED;

-- staff_attendance_external
ALTER TABLE staff_attendance_external RENAME COLUMN created TO created_at;
-- ALTER TABLE staff_attendance_external ADD COLUMN created timestamptz GENERATED ALWAYS AS (created_at) STORED;

DROP TRIGGER IF EXISTS set_timestamp ON staff_attendance_external;
ALTER TABLE staff_attendance_external RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON staff_attendance_external FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
-- ALTER TABLE staff_attendance_external ADD COLUMN updated timestamptz GENERATED ALWAYS AS (updated_at) STORED;

-- staff_attendance_plan
ALTER TABLE staff_attendance_plan RENAME COLUMN created TO created_at;
-- ALTER TABLE staff_attendance_plan ADD COLUMN created timestamptz GENERATED ALWAYS AS (created_at) STORED;

DROP TRIGGER IF EXISTS set_timestamp ON staff_attendance_plan;
ALTER TABLE staff_attendance_plan RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON staff_attendance_plan FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
-- ALTER TABLE staff_attendance_plan ADD COLUMN updated timestamptz GENERATED ALWAYS AS (updated_at) STORED;

-- staff_attendance_realtime
ALTER TABLE staff_attendance_realtime RENAME COLUMN created TO created_at;
-- ALTER TABLE staff_attendance_realtime ADD COLUMN created timestamptz GENERATED ALWAYS AS (created_at) STORED;

DROP TRIGGER IF EXISTS set_timestamp ON staff_attendance_realtime;
ALTER TABLE staff_attendance_realtime RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON staff_attendance_realtime FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
-- ALTER TABLE staff_attendance_realtime ADD COLUMN updated timestamptz GENERATED ALWAYS AS (updated_at) STORED;

-- staff_occupancy_coefficient
ALTER TABLE staff_occupancy_coefficient RENAME COLUMN created TO created_at;
-- ALTER TABLE staff_occupancy_coefficient ADD COLUMN created timestamptz GENERATED ALWAYS AS (created_at) STORED;

DROP TRIGGER IF EXISTS set_timestamp ON staff_occupancy_coefficient;
ALTER TABLE staff_occupancy_coefficient RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON staff_occupancy_coefficient FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
-- ALTER TABLE staff_occupancy_coefficient ADD COLUMN updated timestamptz GENERATED ALWAYS AS (updated_at) STORED;
