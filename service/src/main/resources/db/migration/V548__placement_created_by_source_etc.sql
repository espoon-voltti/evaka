CREATE TYPE placement_source AS ENUM (
    'APPLICATION',
    'SERVICE_APPLICATION',
    'PLACEMENT_TERMINATION',
    'MANUAL'
);

ALTER TABLE placement RENAME COLUMN created TO created_at;
ALTER TABLE placement ALTER COLUMN created_at DROP DEFAULT;

DROP TRIGGER set_timestamp ON placement;
ALTER TABLE placement RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON placement FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();

ALTER TABLE placement
    ADD COLUMN created_by uuid REFERENCES evaka_user(id),
    ADD COLUMN source placement_source,
    ADD COLUMN source_application_id uuid REFERENCES application(id),
    ADD COLUMN source_service_application_id uuid REFERENCES service_application(id),
    ADD CONSTRAINT check$source_application_ref CHECK (source != 'APPLICATION' OR source_application_id IS NOT NULL),
    ADD CONSTRAINT check$source_service_application_ref CHECK (source != 'SERVICE_APPLICATION' OR source_service_application_id IS NOT NULL);

CREATE INDEX fk$placement_source_application_id ON placement (source_application_id) WHERE source_application_id IS NOT NULL;
CREATE INDEX fk$placement_source_service_application_id ON placement (source_service_application_id) WHERE source_service_application_id IS NOT NULL;
