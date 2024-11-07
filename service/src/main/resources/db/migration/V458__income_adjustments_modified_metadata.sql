ALTER TABLE income ADD COLUMN modified_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE income RENAME COLUMN updated_by TO modified_by;
ALTER TABLE income RENAME CONSTRAINT fk$updated_by TO fk$modified_by;

UPDATE income SET
    modified_at = updated_at,
    modified_by = COALESCE(modified_by, '00000000-0000-0000-0000-000000000000'::UUID);

ALTER TABLE income
    ALTER COLUMN modified_at SET NOT NULL,
    ALTER COLUMN updated_at SET DEFAULT NOW();

CREATE TRIGGER set_timestamp BEFORE UPDATE ON income FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();


ALTER TABLE fee_alteration ADD COLUMN modified_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE fee_alteration RENAME COLUMN updated_by TO modified_by;
ALTER TABLE fee_alteration RENAME CONSTRAINT fk$updated_by TO fk$modified_by;

UPDATE fee_alteration SET
    modified_at = updated_at,
    modified_by = COALESCE(modified_by, '00000000-0000-0000-0000-000000000000'::UUID);

ALTER TABLE fee_alteration
    ALTER COLUMN modified_at SET NOT NULL,
    ALTER COLUMN updated_at SET DEFAULT NOW();

CREATE TRIGGER set_timestamp BEFORE UPDATE ON fee_alteration FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
