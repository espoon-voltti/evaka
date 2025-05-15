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

DROP TRIGGER set_timestamp ON assistance_need_decision;
ALTER TABLE assistance_need_decision RENAME COLUMN created TO created_at;
ALTER TABLE assistance_need_decision RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON assistance_need_decision FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();

ALTER TABLE assistance_need_decision_guardian RENAME COLUMN created TO created_at;

DROP TRIGGER set_timestamp ON assistance_need_preschool_decision;
ALTER TABLE assistance_need_preschool_decision RENAME COLUMN created TO created_at;
ALTER TABLE assistance_need_preschool_decision RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON assistance_need_preschool_decision FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();

ALTER TABLE assistance_need_preschool_decision_guardian RENAME COLUMN created TO created_at;

DROP TRIGGER set_timestamp ON assistance_need_voucher_coefficient;
ALTER TABLE assistance_need_voucher_coefficient RENAME COLUMN created TO created_at;
ALTER TABLE assistance_need_voucher_coefficient RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON assistance_need_voucher_coefficient FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
UPDATE assistance_need_voucher_coefficient SET modified_by = coalesce(modified_by, '00000000-0000-0000-0000-000000000000'::UUID);
ALTER TABLE assistance_need_voucher_coefficient ALTER COLUMN modified_by SET NOT NULL;
ALTER TABLE assistance_need_voucher_coefficient ADD CONSTRAINT check_validity_period
    CHECK ((NOT (lower_inf(validity_period) OR upper_inf(validity_period))));

DROP TRIGGER set_timestamp ON attachment;
ALTER TABLE attachment RENAME COLUMN created TO created_at;
ALTER TABLE attachment RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON attachment FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();

DROP TRIGGER set_timestamp ON backup_care;
ALTER TABLE backup_care RENAME COLUMN created TO created_at;
ALTER TABLE backup_care RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON backup_care FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
UPDATE backup_care SET created_at = coalesce(created_at, updated_at, '2000-01-01'::TIMESTAMP WITH TIME ZONE);
ALTER TABLE backup_care ALTER COLUMN created_at SET NOT NULL;
UPDATE backup_care SET updated_at = coalesce(updated_at, created_at, '2000-01-01'::TIMESTAMP WITH TIME ZONE);
ALTER TABLE backup_care ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE backup_care ADD COLUMN modified_at TIMESTAMP WITH TIME ZONE;
UPDATE backup_care SET modified_at = updated_at;
ALTER TABLE backup_care ALTER COLUMN modified_at SET NOT NULL;
ALTER TABLE backup_care ADD COLUMN created_by UUID REFERENCES evaka_user (id) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000'::UUID;
ALTER TABLE backup_care ALTER COLUMN created_by DROP DEFAULT;
CREATE INDEX fk$backup_care_created_by ON backup_care (created_by);
ALTER TABLE backup_care ADD COLUMN modified_by UUID REFERENCES evaka_user (id) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000'::UUID;
ALTER TABLE backup_care ALTER COLUMN modified_by DROP DEFAULT;
CREATE INDEX fk$backup_care_modified_by ON backup_care (modified_by);
