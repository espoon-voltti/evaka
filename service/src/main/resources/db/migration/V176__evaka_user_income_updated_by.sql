INSERT INTO evaka_user (id, type, employee_id, name)
SELECT id, 'EMPLOYEE', id, first_name || ' ' || last_name
FROM employee
WHERE id IN (
    SELECT updated_by FROM income
)
  AND NOT EXISTS (SELECT 1 FROM mobile_device WHERE mobile_device.id = employee.id)
ON CONFLICT DO NOTHING;

INSERT INTO evaka_user (id, type, citizen_id, name)
SELECT id, 'CITIZEN', id, first_name || ' ' || last_name
FROM person
WHERE id IN (
    SELECT updated_by FROM income
)
ON CONFLICT DO NOTHING;

ALTER TABLE income
    ADD COLUMN updated_by_new uuid;

UPDATE income
SET updated_by_new = COALESCE(
    (
        SELECT id
        FROM evaka_user
        WHERE evaka_user.id = income.updated_by
    ),
    '00000000-0000-0000-0000-000000000000');

ALTER TABLE income
    DROP COLUMN updated_by;
ALTER TABLE income
    RENAME COLUMN updated_by_new TO updated_by;
ALTER TABLE income
    ALTER COLUMN updated_by SET NOT NULL,
    ADD CONSTRAINT fk$updated_by FOREIGN KEY (updated_by) REFERENCES evaka_user (id);

CREATE INDEX idx$income_updated_by ON income (updated_by);
