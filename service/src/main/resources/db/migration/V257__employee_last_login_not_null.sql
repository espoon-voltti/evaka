UPDATE employee SET last_login = '2021-01-01 00:00:00+02'
WHERE last_login IS NULL;

ALTER TABLE employee
    ALTER COLUMN last_login SET DEFAULT '2021-01-01 00:00:00+02',
    ALTER COLUMN last_login SET NOT NULL;
