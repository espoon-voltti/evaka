DELETE FROM pairing WHERE mobile_device_id IN (SELECT id FROM mobile_device WHERE deleted IS TRUE);
DELETE FROM mobile_device WHERE deleted IS TRUE;
DROP INDEX idx$mobile_device_unit;

-- Some ACL views depend on the deleted column, so CASCADE must be used
ALTER TABLE mobile_device DROP COLUMN deleted CASCADE;

ALTER TABLE pairing DROP CONSTRAINT pairing_mobile_device_id_fkey;
ALTER TABLE pairing ADD CONSTRAINT fk$mobile_device FOREIGN KEY (mobile_device_id) REFERENCES mobile_device (id) ON DELETE CASCADE;

CREATE INDEX idx$mobile_device_unit ON mobile_device (unit_id);