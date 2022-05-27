CREATE TABLE staff_occupancy_coefficient (
    id          uuid PRIMARY KEY                  DEFAULT ext.uuid_generate_v1mc(),
    created     timestamp with time zone NOT NULL DEFAULT now(),
    updated     timestamp with time zone          DEFAULT now(),
    employee_id uuid                     NOT NULL CONSTRAINT fk$employee REFERENCES employee ON DELETE CASCADE,
    daycare_id  uuid                     NOT NULL CONSTRAINT fk$daycare REFERENCES daycare ON DELETE CASCADE,
    coefficient numeric(4, 2)            NOT NULL
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON staff_occupancy_coefficient FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

CREATE UNIQUE INDEX uniq$staff_occupancy_coefficient_daycare_employee ON staff_occupancy_coefficient (daycare_id, employee_id);

ALTER TABLE staff_attendance_realtime
    ADD COLUMN occupancy_coefficient numeric(4, 2);
UPDATE staff_attendance_realtime SET occupancy_coefficient = 7.0;
ALTER TABLE staff_attendance_realtime
    ALTER COLUMN occupancy_coefficient SET NOT NULL;

ALTER TABLE staff_attendance_external
    ADD COLUMN occupancy_coefficient numeric(4, 2);
UPDATE staff_attendance_external SET occupancy_coefficient = 7.0;
ALTER TABLE staff_attendance_external
    ALTER COLUMN occupancy_coefficient SET NOT NULL;
