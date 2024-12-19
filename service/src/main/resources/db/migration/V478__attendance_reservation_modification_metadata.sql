ALTER TABLE child_attendance
    ADD COLUMN modified_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN modified_by UUID REFERENCES evaka_user;

UPDATE child_attendance SET
    modified_at = COALESCE(created, updated),
    modified_by = COALESCE(modified_by, '00000000-0000-0000-0000-000000000000'::UUID);

ALTER TABLE child_attendance
    ALTER COLUMN modified_at SET NOT NULL,
    ALTER COLUMN modified_by SET NOT NULL;

CREATE INDEX fk$child_attendance_modified_by ON child_attendance(modified_by);


DROP TRIGGER set_timestamp ON child_attendance;
ALTER TABLE child_attendance RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON child_attendance FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();

ALTER TABLE child_attendance RENAME COLUMN created TO created_at;


DROP TRIGGER set_timestamp ON attendance_reservation;
ALTER TABLE attendance_reservation RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON attendance_reservation FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();

ALTER TABLE attendance_reservation RENAME COLUMN created TO created_at;
