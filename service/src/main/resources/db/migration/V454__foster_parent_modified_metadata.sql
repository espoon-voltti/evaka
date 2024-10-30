ALTER TABLE foster_parent
    ADD COLUMN modified_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN created_by UUID REFERENCES evaka_user,
    ADD COLUMN modified_by UUID REFERENCES evaka_user,
    ADD COLUMN create_source create_source,
    ADD COLUMN modify_source modify_source,
    ADD COLUMN created_from_application UUID REFERENCES application;

UPDATE foster_parent SET
    created_by = '00000000-0000-0000-0000-000000000000'::UUID,
    modified_at = COALESCE(updated, created),
    modified_by = '00000000-0000-0000-0000-000000000000'::UUID;

ALTER TABLE foster_parent
    ALTER COLUMN created_by SET NOT NULL,
    ALTER COLUMN modified_at SET NOT NULL,
    ALTER COLUMN modified_by SET NOT NULL;

CREATE INDEX fk$foster_parent_created_by ON foster_parent(created_by);
CREATE INDEX fk$foster_parent_modified_by ON foster_parent(modified_by);
CREATE INDEX fk$foster_parent_created_from_application ON foster_parent(created_from_application);

DROP TRIGGER set_timestamp ON foster_parent;
ALTER TABLE foster_parent RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON foster_parent FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();

ALTER TABLE foster_parent RENAME COLUMN created TO created_at;
