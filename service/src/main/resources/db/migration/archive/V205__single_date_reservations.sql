ALTER TABLE attendance_reservation
    ADD COLUMN date date,
    ADD COLUMN start_time_2 time,
    ADD COLUMN end_time_2 time;

ALTER TABLE attendance_reservation DROP CONSTRAINT attendance_reservation_no_overlap;

UPDATE attendance_reservation SET date = (start_time AT TIME ZONE 'Europe/Helsinki')::date;
UPDATE attendance_reservation SET start_time_2 = (start_time AT TIME ZONE 'Europe/Helsinki')::time;
-- Just use old end time when its date is the same as start time's
UPDATE attendance_reservation SET end_time_2 = (end_time AT TIME ZONE 'Europe/Helsinki')::time
WHERE (end_time AT TIME ZONE 'Europe/Helsinki')::date = (start_time AT TIME ZONE 'Europe/Helsinki')::date;
-- Split reservations into two when end time is on another date compared to start time
UPDATE attendance_reservation SET end_time_2 = '23:59'::time
WHERE (end_time AT TIME ZONE 'Europe/Helsinki')::date != (start_time AT TIME ZONE 'Europe/Helsinki')::date;
INSERT INTO attendance_reservation (created, updated, child_id, start_time, end_time, created_by, date, start_time_2, end_time_2)
(SELECT
    created, updated, child_id, start_time, end_time, created_by,
    (end_time AT TIME ZONE 'Europe/Helsinki')::date AS date,
    '00:00'::time AS start_time_2,
    (end_time AT TIME ZONE 'Europe/Helsinki')::time AS end_time_2
FROM attendance_reservation
WHERE (end_time AT TIME ZONE 'Europe/Helsinki')::date != (start_time AT TIME ZONE 'Europe/Helsinki')::date
AND (end_time AT TIME ZONE 'Europe/Helsinki')::time - '00:00'::time > INTERVAL '5 minutes');

ALTER TABLE attendance_reservation
    DROP COLUMN start_date,
    DROP COLUMN start_time,
    DROP COLUMN end_time;

ALTER TABLE attendance_reservation RENAME COLUMN start_time_2 TO start_time;
ALTER TABLE attendance_reservation RENAME COLUMN end_time_2 TO end_time;

ALTER TABLE attendance_reservation
    ALTER COLUMN date SET NOT NULL,
    ALTER COLUMN start_time SET NOT NULL,
    ALTER COLUMN end_time SET NOT NULL;

ALTER TABLE attendance_reservation ADD CONSTRAINT attendance_reservation_start_before_end CHECK (start_time < end_time);
ALTER TABLE attendance_reservation ADD CONSTRAINT attendance_reservation_no_overlap EXCLUDE USING gist (child_id WITH =, tsrange(date + start_time, date + end_time) WITH &&);
CREATE INDEX idx$reservation_child ON attendance_reservation (child_id);
CREATE INDEX idx$reservation_date_and_times ON attendance_reservation (date, start_time, end_time);
