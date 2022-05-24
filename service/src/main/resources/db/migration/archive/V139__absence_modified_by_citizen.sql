ALTER TABLE absence ALTER COLUMN modified_by DROP NOT NULL;
ALTER TABLE absence RENAME COLUMN modified_by TO modified_by_deprecated;
ALTER TABLE absence ADD COLUMN modified_by_employee_id uuid REFERENCES employee(id);
ALTER TABLE absence ADD COLUMN modified_by_guardian_id uuid REFERENCES person(id);
