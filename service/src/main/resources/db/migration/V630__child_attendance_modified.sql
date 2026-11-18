ALTER TABLE child_attendance
    ADD COLUMN arrived_added_at     TIMESTAMP WITH TIME ZONE,
    ADD COLUMN arrived_added_by     UUID REFERENCES evaka_user,
    ADD COLUMN arrived_modified_at  TIMESTAMP WITH TIME ZONE,
    ADD COLUMN arrived_modified_by  UUID REFERENCES evaka_user,
    ADD COLUMN departed_added_at    TIMESTAMP WITH TIME ZONE,
    ADD COLUMN departed_added_by    UUID REFERENCES evaka_user,
    ADD COLUMN departed_modified_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN departed_modified_by UUID REFERENCES evaka_user;
