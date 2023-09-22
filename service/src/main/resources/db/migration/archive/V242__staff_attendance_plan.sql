CREATE TABLE staff_attendance_plan (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    employee_id uuid NOT NULL REFERENCES employee(id),
    type staff_attendance_type NOT NULL DEFAULT 'PRESENT',
    start_time timestamp with time zone NOT NULL,
    end_time timestamp with time zone NOT NULL,
    description text,

    CONSTRAINT staff_attendance_plan_start_before_end CHECK (start_time < end_time),
    CONSTRAINT staff_attendance_plan_no_overlaps EXCLUDE USING gist (employee_id WITH =, tstzrange(start_time, end_time) WITH &&)
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON staff_attendance_plan FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

CREATE INDEX idx$staff_attendance_plan_employee_id ON staff_attendance_plan (employee_id);
