CREATE INDEX CONCURRENTLY fk$child_attendance_arrived_added_by ON child_attendance (arrived_added_by) WHERE arrived_added_by IS NOT NULL;
CREATE INDEX CONCURRENTLY fk$child_attendance_arrived_modified_by ON child_attendance (arrived_modified_by) WHERE arrived_modified_by IS NOT NULL;
CREATE INDEX CONCURRENTLY fk$child_attendance_departed_added_by ON child_attendance (departed_added_by) WHERE departed_added_by IS NOT NULL;
CREATE INDEX CONCURRENTLY fk$child_attendance_departed_modified_by ON child_attendance (departed_modified_by) WHERE departed_modified_by IS NOT NULL;
