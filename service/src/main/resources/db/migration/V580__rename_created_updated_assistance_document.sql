-- daycare_assistance
ALTER TABLE daycare_assistance RENAME COLUMN created TO created_at;
ALTER TABLE daycare_assistance ADD COLUMN created timestamptz GENERATED ALWAYS AS (created_at) STORED;

DROP TRIGGER IF EXISTS set_timestamp ON daycare_assistance;
ALTER TABLE daycare_assistance RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON daycare_assistance FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
ALTER TABLE daycare_assistance ADD COLUMN updated timestamptz GENERATED ALWAYS AS (updated_at) STORED;

-- other_assistance_measure
ALTER TABLE other_assistance_measure RENAME COLUMN created TO created_at;
ALTER TABLE other_assistance_measure ADD COLUMN created timestamptz GENERATED ALWAYS AS (created_at) STORED;

DROP TRIGGER IF EXISTS set_timestamp ON other_assistance_measure;
ALTER TABLE other_assistance_measure RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON other_assistance_measure FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
ALTER TABLE other_assistance_measure ADD COLUMN updated timestamptz GENERATED ALWAYS AS (updated_at) STORED;

-- preschool_assistance
ALTER TABLE preschool_assistance RENAME COLUMN created TO created_at;
ALTER TABLE preschool_assistance ADD COLUMN created timestamptz GENERATED ALWAYS AS (created_at) STORED;

DROP TRIGGER IF EXISTS set_timestamp ON preschool_assistance;
ALTER TABLE preschool_assistance RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON preschool_assistance FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
ALTER TABLE preschool_assistance ADD COLUMN updated timestamptz GENERATED ALWAYS AS (updated_at) STORED;

-- child_document
ALTER TABLE child_document RENAME COLUMN created TO created_at;
ALTER TABLE child_document ADD COLUMN created timestamptz GENERATED ALWAYS AS (created_at) STORED;

DROP TRIGGER IF EXISTS set_timestamp ON child_document;
ALTER TABLE child_document RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON child_document FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
ALTER TABLE child_document ADD COLUMN updated timestamptz GENERATED ALWAYS AS (updated_at) STORED;

-- document_template
ALTER TABLE document_template RENAME COLUMN created TO created_at;
ALTER TABLE document_template ADD COLUMN created timestamptz GENERATED ALWAYS AS (created_at) STORED;

DROP TRIGGER IF EXISTS set_timestamp ON document_template;
ALTER TABLE document_template RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON document_template FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
ALTER TABLE document_template ADD COLUMN updated timestamptz GENERATED ALWAYS AS (updated_at) STORED;
