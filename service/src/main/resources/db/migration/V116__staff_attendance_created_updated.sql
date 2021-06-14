ALTER TABLE staff_attendance RENAME COLUMN created_at TO created;
ALTER TABLE staff_attendance ADD COLUMN updated timestamptz DEFAULT now();

UPDATE staff_attendance SET updated = created WHERE updated IS NULL;
ALTER TABLE staff_attendance ALTER COLUMN updated SET NOT NULL;

CREATE TRIGGER set_timestamp BEFORE UPDATE ON staff_attendance FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();
