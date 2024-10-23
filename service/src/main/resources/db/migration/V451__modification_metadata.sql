ALTER TABLE application
    ADD COLUMN status_modified_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN status_modified_by UUID REFERENCES evaka_user;

CREATE INDEX fk$application_status_modified_by ON application(status_modified_by);
