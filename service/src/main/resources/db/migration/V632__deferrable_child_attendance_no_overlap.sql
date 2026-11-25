ALTER TABLE child_attendance DROP CONSTRAINT child_attendance_no_overlap;
ALTER TABLE child_attendance
    ADD CONSTRAINT child_attendance_no_overlap EXCLUDE USING gist (child_id WITH =, tsrange(date + start_time, date + end_time) WITH &&) DEFERRABLE INITIALLY IMMEDIATE;
