ALTER TABLE staff_attendance_realtime ADD COLUMN departed_automatically boolean NOT NULL DEFAULT false;
ALTER TABLE staff_attendance_external ADD COLUMN departed_automatically boolean NOT NULL DEFAULT false;

