ALTER TABLE employee ADD COLUMN social_security_number text CONSTRAINT uniq$employee_ssn UNIQUE;
