-- delete daily service times that do not follow the rules
-- note: '(,)'::timerange IS NULL = TRUE
DELETE FROM daily_service_time
WHERE
  regular_times IS NULL AND
  type = 'REGULAR';

DELETE FROM daily_service_time
WHERE
  monday_times IS NULL AND
  tuesday_times IS NULL AND
  wednesday_times IS NULL AND
  thursday_times IS NULL AND
  friday_times IS NULL AND
  saturday_times IS NULL AND
  sunday_times IS NULL AND
  type = 'IRREGULAR';

-- update remaining empty composite types to NULL
UPDATE daily_service_time
SET regular_times = NULL
WHERE (regular_times).start IS NULL OR (regular_times).end IS NULL;

UPDATE daily_service_time
SET monday_times = NULL
WHERE (monday_times).start IS NULL OR (monday_times).end IS NULL;

UPDATE daily_service_time
SET tuesday_times = NULL
WHERE (tuesday_times).start IS NULL OR (tuesday_times).end IS NULL;

UPDATE daily_service_time
SET wednesday_times = NULL
WHERE (wednesday_times).start IS NULL OR (wednesday_times).end IS NULL;

UPDATE daily_service_time
SET thursday_times = NULL
WHERE (thursday_times).start IS NULL OR (thursday_times).end IS NULL;

UPDATE daily_service_time
SET friday_times = NULL
WHERE (friday_times).start IS NULL OR (friday_times).end IS NULL;

UPDATE daily_service_time
SET saturday_times = NULL
WHERE (saturday_times).start IS NULL OR (saturday_times).end IS NULL;

UPDATE daily_service_time
SET sunday_times = NULL
WHERE (sunday_times).start IS NULL OR (sunday_times).end IS NULL;

-- add requirement for start and end time
CREATE DOMAIN timerange_non_nullable_range AS timerange
CHECK (value IS NOT DISTINCT FROM NULL OR ((value).start IS NOT NULL AND (value).end IS NOT NULL));

ALTER TABLE daily_service_time 
  ALTER COLUMN regular_times TYPE timerange_non_nullable_range,
  ALTER COLUMN monday_times TYPE timerange_non_nullable_range,
  ALTER COLUMN tuesday_times TYPE timerange_non_nullable_range,
  ALTER COLUMN wednesday_times TYPE timerange_non_nullable_range,
  ALTER COLUMN thursday_times TYPE timerange_non_nullable_range,
  ALTER COLUMN friday_times TYPE timerange_non_nullable_range,
  ALTER COLUMN saturday_times TYPE timerange_non_nullable_range,
  ALTER COLUMN sunday_times TYPE timerange_non_nullable_range;
