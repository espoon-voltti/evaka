ALTER TABLE employee DROP CONSTRAINT employee_employee_number_key;
CREATE UNIQUE INDEX employee_employee_number_key
    ON employee (employee_number)
    WHERE active = TRUE;
