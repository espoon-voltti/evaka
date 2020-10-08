CREATE TABLE child_attendance (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    child_id uuid NOT NULL REFERENCES child(id),
    arrived timestamp with time zone NOT NULL,
    departed timestamp with time zone,

    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,

    CONSTRAINT child_attendance_start_before_end CHECK (arrived <= departed),
    CONSTRAINT child_attendance_no_overlaps EXCLUDE USING gist (child_id WITH =, tstzrange(arrived, departed) WITH &&)
);

CREATE INDEX child_attendance_child_times_idx ON child_attendance(child_id, arrived, departed);

CREATE UNIQUE INDEX uniq$child_attendance_active ON child_attendance(child_id) WHERE (departed IS NULL);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON child_attendance FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();
