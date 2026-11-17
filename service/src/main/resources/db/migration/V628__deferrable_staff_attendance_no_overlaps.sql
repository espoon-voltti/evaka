ALTER TABLE staff_attendance_realtime DROP CONSTRAINT staff_attendance_no_overlaps;
ALTER TABLE staff_attendance_realtime
    ADD CONSTRAINT staff_attendance_no_overlaps EXCLUDE USING gist (employee_id WITH =, tstzrange(arrived, departed) WITH &&) DEFERRABLE INITIALLY IMMEDIATE;
