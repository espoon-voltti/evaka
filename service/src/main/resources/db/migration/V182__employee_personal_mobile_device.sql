ALTER TABLE pairing ALTER COLUMN unit_id DROP NOT NULL;
ALTER TABLE pairing ADD COLUMN employee_id uuid REFERENCES employee(id);
ALTER TABLE pairing ADD CONSTRAINT pairing_non_null_reference CHECK (num_nonnulls(unit_id, employee_id) = 1);

ALTER TABLE mobile_device ALTER COLUMN unit_id DROP NOT NULL;
ALTER TABLE mobile_device ADD COLUMN employee_id uuid REFERENCES employee(id);
ALTER TABLE mobile_device ADD CONSTRAINT mobile_device_non_null_reference CHECK (num_nonnulls(unit_id, employee_id) = 1);
