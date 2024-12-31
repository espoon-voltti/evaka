ALTER TABLE staff_attendance_realtime
    ADD COLUMN arrived_added_at     TIMESTAMP WITH TIME ZONE,
    ADD COLUMN arrived_added_by     UUID REFERENCES evaka_user,
    ADD COLUMN arrived_modified_at  TIMESTAMP WITH TIME ZONE,
    ADD COLUMN arrived_modified_by  UUID REFERENCES evaka_user,
    ADD COLUMN departed_added_at    TIMESTAMP WITH TIME ZONE,
    ADD COLUMN departed_added_by    UUID REFERENCES evaka_user,
    ADD COLUMN departed_modified_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN departed_modified_by UUID REFERENCES evaka_user;
CREATE INDEX fk$staff_attendance_realtime_arrived_added_by ON staff_attendance_realtime (arrived_added_by);
CREATE INDEX fk$staff_attendance_realtime_arrived_modified_by ON staff_attendance_realtime (arrived_modified_by);
CREATE INDEX fk$staff_attendance_realtime_departed_added_by ON staff_attendance_realtime (departed_added_by);
CREATE INDEX fk$staff_attendance_realtime_departed_modified_by ON staff_attendance_realtime (departed_modified_by);
