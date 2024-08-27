CREATE INDEX CONCURRENTLY idx$child_attendance_unit_range ON child_attendance USING gist(unit_id, tstzrange(arrived, departed));
CREATE INDEX CONCURRENTLY idx$child_attendance_child_range ON child_attendance USING gist(child_id, tstzrange(arrived, departed));
