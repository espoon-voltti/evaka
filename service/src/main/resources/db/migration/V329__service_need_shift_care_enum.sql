CREATE TYPE shift_care_type AS ENUM (
    'FULL',
    'INTERMITTENT',
    'NONE'
);

ALTER TABLE service_need
    ADD COLUMN shift_care_temp shift_care_type NOT NULL DEFAULT 'NONE';

ALTER TABLE service_need DISABLE TRIGGER set_timestamp;

UPDATE service_need
SET shift_care_temp = 'FULL'
WHERE shift_care IS TRUE;

ALTER TABLE service_need ENABLE TRIGGER set_timestamp;

ALTER TABLE service_need
    DROP COLUMN shift_care;

ALTER TABLE service_need
    RENAME COLUMN shift_care_temp TO shift_care;
