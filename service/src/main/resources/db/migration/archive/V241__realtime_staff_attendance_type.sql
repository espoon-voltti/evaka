CREATE TYPE staff_attendance_type AS ENUM (
    'PRESENT',
    'OTHER_WORK',
    'TRAINING',
    'OVERTIME',
    'JUSTIFIED_CHANGE'
);

ALTER TABLE staff_attendance_realtime ADD COLUMN type staff_attendance_type NOT NULL DEFAULT 'PRESENT';
