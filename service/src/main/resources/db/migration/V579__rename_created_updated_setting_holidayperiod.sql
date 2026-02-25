-- setting
ALTER TABLE setting RENAME COLUMN created TO created_at;
-- ALTER TABLE setting ADD COLUMN created timestamptz GENERATED ALWAYS AS (created_at) STORED;

DROP TRIGGER IF EXISTS set_timestamp ON setting;
ALTER TABLE setting RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON setting FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
-- ALTER TABLE setting ADD COLUMN updated timestamptz GENERATED ALWAYS AS (updated_at) STORED;

-- holiday_period
ALTER TABLE holiday_period RENAME COLUMN created TO created_at;
-- ALTER TABLE holiday_period ADD COLUMN created timestamptz GENERATED ALWAYS AS (created_at) STORED;

DROP TRIGGER IF EXISTS set_timestamp ON holiday_period;
ALTER TABLE holiday_period RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON holiday_period FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
-- ALTER TABLE holiday_period ADD COLUMN updated timestamptz GENERATED ALWAYS AS (updated_at) STORED;

-- holiday_period_questionnaire
ALTER TABLE holiday_period_questionnaire RENAME COLUMN created TO created_at;
-- ALTER TABLE holiday_period_questionnaire ADD COLUMN created timestamptz GENERATED ALWAYS AS (created_at) STORED;

DROP TRIGGER IF EXISTS set_timestamp ON holiday_period_questionnaire;
ALTER TABLE holiday_period_questionnaire RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON holiday_period_questionnaire FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
-- ALTER TABLE holiday_period_questionnaire ADD COLUMN updated timestamptz GENERATED ALWAYS AS (updated_at) STORED;

-- holiday_questionnaire_answer
ALTER TABLE holiday_questionnaire_answer RENAME COLUMN created TO created_at;
-- ALTER TABLE holiday_questionnaire_answer ADD COLUMN created timestamptz GENERATED ALWAYS AS (created_at) STORED;

DROP TRIGGER IF EXISTS set_timestamp ON holiday_questionnaire_answer;
ALTER TABLE holiday_questionnaire_answer RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON holiday_questionnaire_answer FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
-- ALTER TABLE holiday_questionnaire_answer ADD COLUMN updated timestamptz GENERATED ALWAYS AS (updated_at) STORED;
