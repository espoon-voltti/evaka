-- Make shift_care_operation_times nullable for units that do not have shift care

ALTER TABLE daycare ALTER COLUMN shift_care_operation_times DROP NOT NULL;

ALTER TABLE daycare DROP CONSTRAINT check$full_week_shift_care_operation_times;
ALTER TABLE daycare ADD CONSTRAINT check$full_week_shift_care_operation_times
    CHECK ( shift_care_operation_times IS NULL OR cardinality(shift_care_operation_times) = 7 );

ALTER TABLE daycare DROP COLUMN shift_care_operation_days;
ALTER TABLE daycare
    ADD COLUMN shift_care_operation_days integer[] GENERATED ALWAYS AS (
        CASE
            WHEN shift_care_operation_times IS NOT NULL
            THEN
                array_remove(
                    array [
                        CASE WHEN daycare.shift_care_operation_times[1] IS NOT NULL THEN 1 END,
                        CASE WHEN daycare.shift_care_operation_times[2] IS NOT NULL THEN 2 END,
                        CASE WHEN daycare.shift_care_operation_times[3] IS NOT NULL THEN 3 END,
                        CASE WHEN daycare.shift_care_operation_times[4] IS NOT NULL THEN 4 END,
                        CASE WHEN daycare.shift_care_operation_times[5] IS NOT NULL THEN 5 END,
                        CASE WHEN daycare.shift_care_operation_times[6] IS NOT NULL THEN 6 END,
                        CASE WHEN daycare.shift_care_operation_times[7] IS NOT NULL THEN 7 END
                    ],
                    NULL
                )
        END
    ) STORED;

-- Units without shift care should have shift_care_operation_times = NULL
UPDATE daycare
SET shift_care_operation_times = NULL
WHERE round_the_clock = FALSE;

-- Units with shift care must have shift_care_operation_times. Use normal operation times as defaults.
UPDATE daycare
SET shift_care_operation_times = operation_times
WHERE round_the_clock AND
      (shift_care_operation_times IS NULL OR
       shift_care_operation_times = '{NULL,NULL,NULL,NULL,NULL,NULL,NULL}'::timerange_non_nullable_range[]);

-- Change round_the_clock into a renamed generated column
ALTER TABLE daycare DROP round_the_clock;
ALTER TABLE daycare ADD COLUMN provides_shift_care boolean GENERATED ALWAYS AS (
    shift_care_operation_times IS NOT NULL
) STORED;

-- Add an explicit column for whether the unit is open on holidays
ALTER TABLE daycare ADD COLUMN shift_care_open_on_holidays boolean NOT NULL DEFAULT FALSE;
UPDATE daycare SET shift_care_open_on_holidays = provides_shift_care AND
    (cardinality(coalesce(shift_care_operation_days, operation_days)) = 7);
ALTER TABLE daycare ADD CONSTRAINT check$shift_care_open_on_holidays_only_for_shift_care CHECK (
    provides_shift_care = TRUE OR shift_care_open_on_holidays = FALSE
);
