ALTER TABLE child_attendance
    ADD COLUMN date date,
    ADD COLUMN start_time time,
    ADD COLUMN end_time time DEFAULT NULL;

WITH attendance AS (
    DELETE FROM child_attendance RETURNING *
)
INSERT INTO child_attendance (child_id, arrived, departed, created, updated, unit_id, date, start_time, end_time)
SELECT child_id, date + start_time, date + end_time, created, updated, unit_id, date, start_time, end_time
FROM (
    SELECT
        child_id, created, updated, unit_id,
        t::date AS date,
        date_trunc('minute', (CASE WHEN (arrived AT TIME ZONE 'Europe/Helsinki')::date < t::date THEN '00:00'::time ELSE (arrived AT TIME ZONE 'Europe/Helsinki')::time END))::time AS start_time,
        date_trunc('minute', (CASE WHEN (departed AT TIME ZONE 'Europe/Helsinki')::date > t::date THEN '23:59'::time ELSE (departed AT TIME ZONE 'Europe/Helsinki')::time END))::time AS end_time
    FROM attendance
    JOIN LATERAL (SELECT generate_series(arrived::date, departed::date, '1 day') t) t ON true
    WHERE departed IS NOT NULL

    UNION ALL

    SELECT
        child_id, created, updated, unit_id,
        (arrived AT TIME ZONE 'Europe/Helsinki')::date,
        (arrived AT TIME ZONE 'Europe/Helsinki')::time,
        NULL
    FROM attendance
    WHERE departed IS NULL
) a;

ALTER TABLE child_attendance
    DROP CONSTRAINT child_attendance_no_overlaps,
    DROP COLUMN arrived,
    DROP COLUMN departed;

ALTER TABLE child_attendance
    ALTER COLUMN date SET NOT NULL,
    ALTER COLUMN start_time SET NOT NULL;

ALTER TABLE child_attendance ADD CONSTRAINT child_attendance_time_resolution CHECK (EXTRACT(SECOND FROM start_time) = 0 AND EXTRACT(SECOND FROM end_time) = 0);
ALTER TABLE child_attendance ADD CONSTRAINT child_attendance_start_before_end CHECK (start_time < end_time);
ALTER TABLE child_attendance ADD CONSTRAINT child_attendance_no_overlap EXCLUDE USING gist (child_id WITH =, tsrange(date + start_time, date + end_time) WITH &&);
CREATE INDEX idx$attendance_child ON child_attendance (child_id);
CREATE INDEX idx$attendance_date_and_times ON child_attendance (date, start_time, end_time);
