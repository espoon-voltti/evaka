ALTER TABLE attendance_reservation DROP CONSTRAINT attendance_reservation_no_overlap;

ALTER TABLE attendance_reservation
    ALTER COLUMN start_time DROP NOT NULL,
    ALTER COLUMN end_time DROP NOT NULL,
    ADD CONSTRAINT attendance_reservation_times_consistency CHECK (
        (start_time IS NOT NULL AND end_time IS NOT NULL) OR
        (start_time IS NULL AND end_time IS NULL)
    ),
    ADD CONSTRAINT attendance_reservation_no_overlap_with_times
        EXCLUDE USING gist (child_id WITH =, tsrange(date + start_time, date + end_time) WITH &&)
        WHERE (start_time IS NOT NULL AND end_time IS NOT NULL),
    ADD CONSTRAINT attendance_reservation_no_overlap_without_times
        EXCLUDE (child_id WITH =, date WITH =)
        WHERE (start_time IS NULL AND end_time IS NULL);
