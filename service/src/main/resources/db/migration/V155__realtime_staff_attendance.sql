DROP TABLE IF EXISTS staff_attendance_2;
DROP TABLE IF EXISTS staff_attendance_realtime;
DROP TABLE IF EXISTS staff_attendance_external;
DROP TABLE IF EXISTS staff_attendance_externals;

CREATE TABLE staff_attendance_realtime(
                                          id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
                                          created timestamp with time zone DEFAULT now() NOT NULL,
                                          updated timestamp with time zone DEFAULT now() NOT NULL,
                                          employee_id uuid NOT NULL REFERENCES employee(id),
                                          group_id uuid NOT NULL REFERENCES daycare_group(id),
                                          arrived timestamp with time zone NOT NULL,
                                          departed timestamp with time zone,

                                          CONSTRAINT staff_attendance_start_before_end CHECK (arrived < departed),
                                          CONSTRAINT staff_attendance_no_overlaps EXCLUDE USING gist (employee_id WITH =, tstzrange(arrived, departed) WITH &&)
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON staff_attendance_realtime FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

CREATE INDEX idx$staff_attendance_realtime_employee_id ON staff_attendance_realtime(employee_id);
CREATE INDEX idx$staff_attendance_realtime_group_id_times ON staff_attendance_realtime(group_id, arrived, departed);
CREATE INDEX idx$staff_attendance_realtime_group_id_times_gist ON staff_attendance_realtime USING GIST (group_id, tstzrange(arrived, departed));
CREATE INDEX idx$staff_attendance_realtime_group_id_present ON staff_attendance_realtime(group_id) WHERE departed IS NULL;

CREATE TABLE staff_attendance_external(
                                          id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
                                          created timestamp with time zone DEFAULT now() NOT NULL,
                                          updated timestamp with time zone DEFAULT now() NOT NULL,
                                          name text NOT NULL,
                                          group_id uuid NOT NULL REFERENCES daycare_group(id),
                                          arrived timestamp with time zone NOT NULL,
                                          departed timestamp with time zone,

                                          CONSTRAINT staff_attendance_external_start_before_end CHECK (arrived < departed)
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON staff_attendance_external FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

CREATE INDEX idx$staff_attendance_external_group_id_times ON staff_attendance_external(group_id, arrived, departed);
CREATE INDEX idx$staff_attendance_external_group_id_times_gist ON staff_attendance_external USING GIST (group_id, tstzrange(arrived, departed));
CREATE INDEX idx$staff_attendance_external_group_id_present ON staff_attendance_external(group_id) WHERE departed IS NULL;
