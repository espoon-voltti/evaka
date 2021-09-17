DROP TABLE IF EXISTS attendance_reservation;

CREATE TABLE attendance_reservation (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    child_id uuid NOT NULL REFERENCES person(id),
    start_time timestamp with time zone NOT NULL,
    end_time timestamp with time zone NOT NULL,
    start_date date GENERATED ALWAYS AS ((start_time AT TIME ZONE 'Europe/Helsinki')::date) STORED,
    created_by_guardian_id uuid REFERENCES person(id),
    created_by_employee_id uuid REFERENCES employee(id),
    CONSTRAINT attendance_reservation_start_before_end CHECK (start_time < end_time),
    CONSTRAINT attendance_reservation_max_24_hours CHECK (end_time - interval '1 day' <= start_time),
    CONSTRAINT attendance_reservation_no_overlap EXCLUDE USING gist (child_id WITH =, tstzrange(start_time, end_time) WITH &&),
    CONSTRAINT attendance_reservation_created_by_someone CHECK ((created_by_guardian_id IS NULL) != (created_by_employee_id IS NULL))
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON attendance_reservation FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();
