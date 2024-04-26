UPDATE employee_pin SET locked = FALSE WHERE locked IS NULL;
ALTER TABLE employee_pin ALTER COLUMN locked SET NOT NULL;
