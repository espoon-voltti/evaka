CREATE FUNCTION between_start_and_end(tstzrange, timestamptz) RETURNS bool PARALLEL SAFE AS $$
SELECT ($2 > coalesce(lower($1), '-infinity') AND $2 < coalesce(upper($1), 'infinity'))
   OR (lower_inc($1) AND $2 = coalesce(lower($1), '-infinity'))
   OR (upper_inc($1) AND $2 = coalesce(upper($1), 'infinity'))
$$ LANGUAGE sql IMMUTABLE;
COMMENT ON FUNCTION between_start_and_end(tstzrange, timestamptz) IS
    'Checks if the given range contains the given timestamp.';

CREATE INDEX idx$staff_attendance_realtime_employee_id_times ON staff_attendance_realtime(employee_id, arrived, departed);

CREATE INDEX idx$staff_attendance_external_name_times ON staff_attendance_external(name, arrived, departed);
