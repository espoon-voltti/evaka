DROP TRIGGER set_timestamp ON application_note;
ALTER TABLE application_note RENAME COLUMN created TO created_at;
ALTER TABLE application_note RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON application_note FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
ALTER TABLE application_note ADD COLUMN modified_at TIMESTAMP WITH TIME ZONE;
UPDATE application_note SET modified_at = updated_at;
ALTER TABLE application_note ALTER COLUMN modified_at SET NOT NULL;
ALTER TABLE application_note RENAME COLUMN updated_by TO modified_by;

DROP TRIGGER set_timestamp ON application_other_guardian;
ALTER TABLE application_other_guardian RENAME COLUMN created TO created_at;
ALTER TABLE application_other_guardian RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON application_other_guardian FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
