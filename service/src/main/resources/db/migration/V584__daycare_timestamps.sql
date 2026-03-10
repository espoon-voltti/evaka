-- care_area
ALTER TABLE care_area RENAME COLUMN created TO created_at;
ALTER TABLE care_area ADD COLUMN created timestamptz GENERATED ALWAYS AS (created_at) STORED;

DROP TRIGGER IF EXISTS set_timestamp ON care_area;
ALTER TABLE care_area RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON care_area FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
ALTER TABLE care_area ADD COLUMN updated timestamptz GENERATED ALWAYS AS (updated_at) STORED;

-- daycare
ALTER TABLE daycare RENAME COLUMN created TO created_at;
ALTER TABLE daycare ADD COLUMN created timestamptz GENERATED ALWAYS AS (created_at) STORED;

DROP TRIGGER IF EXISTS set_timestamp ON daycare;
ALTER TABLE daycare RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON daycare FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
ALTER TABLE daycare ADD COLUMN updated timestamptz GENERATED ALWAYS AS (updated_at) STORED;

-- daycare_acl
ALTER TABLE daycare_acl RENAME COLUMN created TO created_at;
ALTER TABLE daycare_acl ADD COLUMN created timestamptz GENERATED ALWAYS AS (created_at) STORED;

DROP TRIGGER IF EXISTS set_timestamp ON daycare_acl;
ALTER TABLE daycare_acl RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON daycare_acl FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
ALTER TABLE daycare_acl ADD COLUMN updated timestamptz GENERATED ALWAYS AS (updated_at) STORED;

-- daycare_caretaker
ALTER TABLE daycare_caretaker RENAME COLUMN created TO created_at;
ALTER TABLE daycare_caretaker ADD COLUMN created timestamptz GENERATED ALWAYS AS (created_at) STORED;

DROP TRIGGER IF EXISTS set_timestamp ON daycare_caretaker;
ALTER TABLE daycare_caretaker RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON daycare_caretaker FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
ALTER TABLE daycare_caretaker ADD COLUMN updated timestamptz GENERATED ALWAYS AS (updated_at) STORED;

-- daycare_group_acl
ALTER TABLE daycare_group_acl RENAME COLUMN created TO created_at;
ALTER TABLE daycare_group_acl ADD COLUMN created timestamptz GENERATED ALWAYS AS (created_at) STORED;

DROP TRIGGER IF EXISTS set_timestamp ON daycare_group_acl;
ALTER TABLE daycare_group_acl RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON daycare_group_acl FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
ALTER TABLE daycare_group_acl ADD COLUMN updated timestamptz GENERATED ALWAYS AS (updated_at) STORED;

-- daycare_group_placement
ALTER TABLE daycare_group_placement RENAME COLUMN created TO created_at;
ALTER TABLE daycare_group_placement ADD COLUMN created timestamptz GENERATED ALWAYS AS (created_at) STORED;

DROP TRIGGER IF EXISTS set_timestamp ON daycare_group_placement;
ALTER TABLE daycare_group_placement RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON daycare_group_placement FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
ALTER TABLE daycare_group_placement ADD COLUMN updated timestamptz GENERATED ALWAYS AS (updated_at) STORED;

-- club_term
ALTER TABLE club_term RENAME COLUMN created TO created_at;
ALTER TABLE club_term ADD COLUMN created timestamptz GENERATED ALWAYS AS (created_at) STORED;

DROP TRIGGER IF EXISTS set_timestamp ON club_term;
ALTER TABLE club_term RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON club_term FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
ALTER TABLE club_term ADD COLUMN updated timestamptz GENERATED ALWAYS AS (updated_at) STORED;

-- preschool_term
ALTER TABLE preschool_term RENAME COLUMN created TO created_at;
ALTER TABLE preschool_term ADD COLUMN created timestamptz GENERATED ALWAYS AS (created_at) STORED;

DROP TRIGGER IF EXISTS set_timestamp ON preschool_term;
ALTER TABLE preschool_term RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON preschool_term FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
ALTER TABLE preschool_term ADD COLUMN updated timestamptz GENERATED ALWAYS AS (updated_at) STORED;
