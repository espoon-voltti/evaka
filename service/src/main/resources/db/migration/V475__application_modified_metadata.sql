ALTER TABLE application RENAME COLUMN form_modified TO modified_at;

ALTER TABLE application ADD COLUMN modified_by UUID REFERENCES evaka_user;

UPDATE application SET
    created_by = COALESCE(created_by, '00000000-0000-0000-0000-000000000000'::UUID),
    modified_by = COALESCE(modified_by, created_by, '00000000-0000-0000-0000-000000000000'::UUID);

ALTER TABLE application
    ALTER COLUMN created_by SET NOT NULL,
    ALTER COLUMN modified_by SET NOT NULL;

CREATE INDEX fk$application_modified_by ON application(modified_by);


ALTER TABLE application RENAME COLUMN created TO created_at;

DROP TRIGGER set_timestamp ON application;
ALTER TABLE application RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON application FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
