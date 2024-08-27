ALTER TABLE child_attendance
    ADD COLUMN arrived timestamptz NOT NULL GENERATED ALWAYS AS ((date + start_time) AT TIME ZONE 'Europe/Helsinki') STORED,
    ADD COLUMN departed timestamptz GENERATED ALWAYS AS ((date + end_time) AT TIME ZONE 'Europe/Helsinki') STORED;
