ALTER TABLE income_statement ADD COLUMN handler_id uuid CONSTRAINT fk$employee REFERENCES employee (id);
