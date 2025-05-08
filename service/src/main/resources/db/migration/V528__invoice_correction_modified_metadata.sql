ALTER TABLE invoice_correction RENAME COLUMN created TO created_at;

ALTER TABLE invoice_correction ADD COLUMN created_by UUID REFERENCES evaka_user(id) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000'::UUID;
ALTER TABLE invoice_correction ALTER COLUMN created_by DROP DEFAULT;
CREATE INDEX fk$invoice_correction_created_by ON invoice_correction(created_by);

DROP TRIGGER set_timestamp ON invoice_correction;
ALTER TABLE invoice_correction RENAME COLUMN updated TO updated_at;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON invoice_correction FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();

ALTER TABLE invoice_correction ADD COLUMN modified_at TIMESTAMP WITH TIME ZONE;
UPDATE invoice_correction SET modified_at = updated_at;
ALTER TABLE invoice_correction ALTER COLUMN modified_at SET NOT NULL;

ALTER TABLE invoice_correction ADD COLUMN modified_by UUID REFERENCES evaka_user(id) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000'::UUID;
ALTER TABLE invoice_correction ALTER COLUMN modified_by DROP DEFAULT;
CREATE INDEX fk$invoice_correction_modified_by ON invoice_correction(modified_by);

CREATE INDEX fk$invoice_correction_unit_id ON invoice_correction(unit_id);
