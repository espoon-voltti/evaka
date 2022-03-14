CREATE UNIQUE INDEX CONCURRENTLY uniq$child_attendance_active ON child_attendance (child_id) WHERE end_time IS NULL;

CREATE INDEX CONCURRENTLY idx$child_attendance_unit ON child_attendance (unit_id, date);
CREATE INDEX CONCURRENTLY idx$child_attendance_child ON child_attendance (child_id, date);
