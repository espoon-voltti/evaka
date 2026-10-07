CREATE TYPE titania_error_type AS ENUM ('OVERLAPPING_SHIFT', 'ZERO_LENGTH_SHIFT', 'REVERSED_SHIFT');

ALTER TABLE titania_errors
    ADD COLUMN error_type titania_error_type NOT NULL DEFAULT 'OVERLAPPING_SHIFT',
    ALTER COLUMN overlapping_shift_begins DROP NOT NULL,
    ALTER COLUMN overlapping_shift_ends DROP NOT NULL;

ALTER TABLE titania_errors
    ALTER COLUMN error_type DROP DEFAULT,
    ADD CONSTRAINT check$overlapping_shift_only_for_overlaps CHECK (
        (error_type = 'OVERLAPPING_SHIFT') = (overlapping_shift_begins IS NOT NULL) AND
        (error_type = 'OVERLAPPING_SHIFT') = (overlapping_shift_ends IS NOT NULL)
    );
