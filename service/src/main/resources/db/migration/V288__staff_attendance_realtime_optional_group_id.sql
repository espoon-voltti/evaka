ALTER TABLE staff_attendance_realtime ALTER COLUMN group_id DROP NOT NULL;
ALTER TABLE staff_attendance_realtime ADD CONSTRAINT check_group_id_if_working_in_group CHECK (group_id IS NOT NULL OR type = ANY('{TRAINING,OTHER_WORK}'));
