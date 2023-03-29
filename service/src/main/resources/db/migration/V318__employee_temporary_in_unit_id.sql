ALTER TABLE employee ADD COLUMN temporary_in_unit_id UUID REFERENCES daycare;
CREATE INDEX idx$employee_temporary_in_unit_id ON employee (temporary_in_unit_id);
