CREATE TYPE timerange AS (
  "start" time,
  "end" time
);

ALTER TABLE daily_service_time
  ADD COLUMN validity_period daterange NOT NULL DEFAULT daterange(current_date, NULL, '[]'),
  ADD COLUMN regular_times timerange,
  ADD COLUMN monday_times timerange,
  ADD COLUMN tuesday_times timerange,
  ADD COLUMN wednesday_times timerange,
  ADD COLUMN thursday_times timerange,
  ADD COLUMN friday_times timerange,
  ADD COLUMN saturday_times timerange,
  ADD COLUMN sunday_times timerange,
  ADD CONSTRAINT check$no_validity_period_overlap EXCLUDE USING gist (
    child_id WITH =,
    validity_period WITH &&
  ),
  DROP CONSTRAINT no_partial_time_ranges;

UPDATE daily_service_time SET regular_times = (regular_start::time, regular_end::time)::timerange WHERE type = 'REGULAR';

UPDATE daily_service_time
SET
  monday_times = (monday_start::time, monday_end::time)::timerange,
  tuesday_times = (tuesday_start::time, tuesday_end::time)::timerange,
  wednesday_times = (wednesday_start::time, wednesday_end::time)::timerange,
  thursday_times = (thursday_start::time, thursday_end::time)::timerange,
  friday_times = (friday_start::time, friday_end::time)::timerange,
  saturday_times = (saturday_start::time, saturday_end::time)::timerange,
  sunday_times = (sunday_start::time, sunday_end::time)::timerange
WHERE type = 'IRREGULAR';

ALTER TABLE daily_service_time
  DROP COLUMN monday_start,
  DROP COLUMN monday_end,
  DROP COLUMN tuesday_start,
  DROP COLUMN tuesday_end,
  DROP COLUMN wednesday_start,
  DROP COLUMN wednesday_end,
  DROP COLUMN thursday_start,
  DROP COLUMN thursday_end,
  DROP COLUMN friday_start,
  DROP COLUMN friday_end,
  DROP COLUMN saturday_start,
  DROP COLUMN saturday_end,
  DROP COLUMN sunday_start,
  DROP COLUMN sunday_end,
  ALTER COLUMN validity_period DROP DEFAULT,
  ADD CONSTRAINT check$regular_daily_service_times CHECK (
    (type != 'REGULAR') OR (
      regular_times IS NOT NULL AND
      monday_times IS NULL AND
      tuesday_times IS NULL AND
      wednesday_times IS NULL AND
      thursday_times IS NULL AND
      friday_times IS NULL AND
      saturday_times IS NULL AND
      sunday_times IS NULL
    )
  ),
  ADD CONSTRAINT check$irregular_daily_service_times CHECK (
    (type != 'IRREGULAR') OR (
      regular_times IS NULL AND
      num_nonnulls(
        monday_times, tuesday_times, wednesday_times, thursday_times, friday_times, saturday_times, sunday_times
      ) >= 1
    )
  ),
  ADD CONSTRAINT check$variable_daily_service_times CHECK (
    (type != 'VARIABLE_TIME') OR (
      regular_times IS NULL AND
      monday_times IS NULL AND
      tuesday_times IS NULL AND
      wednesday_times IS NULL AND
      thursday_times IS NULL AND
      friday_times IS NULL AND
      saturday_times IS NULL AND
      sunday_times IS NULL
    )
  );

DROP INDEX uniq$daily_service_time_child;
CREATE INDEX idx$daily_service_time_child_id ON daily_service_time (child_id);

CREATE INDEX idx$daily_service_time_validity_period ON daily_service_time USING GIST (validity_period);
