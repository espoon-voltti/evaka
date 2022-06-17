CREATE TYPE public.staff_attendance_type AS ENUM (
    'WORK',
    'TRAINING',
    'WORK_JUSTIFIED_CHANGE'
    );

ALTER TABLE staff_attendance_realtime ADD COLUMN attendance_type staff_attendance_type default 'WORK';