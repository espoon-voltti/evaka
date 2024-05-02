ALTER TABLE daycare
    ADD COLUMN shift_care_operation_times timerange_non_nullable_range[] NOT NULL DEFAULT '{NULL,NULL,NULL,NULL,NULL,NULL,NULL}'::timerange_non_nullable_range[],
    ADD CONSTRAINT check$full_week_shift_care_operation_times CHECK ( cardinality(shift_care_operation_times) = 7 );

ALTER TABLE daycare
    ADD COLUMN shift_care_operation_days integer[] GENERATED ALWAYS AS (
                                              array_remove(array [
                                                               CASE WHEN daycare.shift_care_operation_times[1] IS NOT NULL THEN 1 END,
                                                               CASE WHEN daycare.shift_care_operation_times[2] IS NOT NULL THEN 2 END,
                                                               CASE WHEN daycare.shift_care_operation_times[3] IS NOT NULL THEN 3 END,
                                                               CASE WHEN daycare.shift_care_operation_times[4] IS NOT NULL THEN 4 END,
                                                               CASE WHEN daycare.shift_care_operation_times[5] IS NOT NULL THEN 5 END,
                                                               CASE WHEN daycare.shift_care_operation_times[6] IS NOT NULL THEN 6 END,
                                                               CASE WHEN daycare.shift_care_operation_times[7] IS NOT NULL THEN 7 END
                                                               ], NULL)
                                              ) STORED;
