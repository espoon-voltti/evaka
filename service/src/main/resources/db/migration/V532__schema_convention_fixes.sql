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

DROP TRIGGER set_timestamp ON assistance_action;
ALTER TABLE assistance_action RENAME COLUMN created TO created_at;
ALTER TABLE assistance_action RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON assistance_action FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
ALTER TABLE assistance_action ADD COLUMN modified_at TIMESTAMP WITH TIME ZONE;
UPDATE assistance_action SET modified_at = updated_at;
ALTER TABLE assistance_action ALTER COLUMN modified_at SET NOT NULL;
ALTER TABLE assistance_action RENAME COLUMN updated_by TO modified_by;

DROP TRIGGER set_timestamp ON assistance_action_option;
ALTER TABLE assistance_action_option RENAME COLUMN created TO created_at;
ALTER TABLE assistance_action_option RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON assistance_action_option FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();

ALTER TABLE assistance_action_option_ref RENAME COLUMN created TO created_at;
CREATE INDEX fk$assistance_action_option_ref_option_id ON assistance_action_option_ref (option_id);

DROP TRIGGER set_timestamp ON assistance_factor;
ALTER TABLE assistance_factor RENAME COLUMN created TO created_at;
ALTER TABLE assistance_factor RENAME COLUMN updated TO updated_at;
ALTER TABLE assistance_factor RENAME COLUMN modified TO modified_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON assistance_factor FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
CREATE INDEX fk$assistance_factor_modified_by ON assistance_factor (modified_by);
