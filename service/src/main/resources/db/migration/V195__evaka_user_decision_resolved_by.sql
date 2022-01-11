-- create potentially missing evaka_user rows
INSERT INTO evaka_user (id, type, employee_id, name)
SELECT id, 'EMPLOYEE', id, first_name || ' ' || last_name
FROM employee
WHERE id IN (
    SELECT resolved_by FROM decision
)
  AND NOT EXISTS (SELECT 1 FROM mobile_device WHERE mobile_device.id = employee.id)
ON CONFLICT DO NOTHING;

INSERT INTO evaka_user (id, type, citizen_id, name)
SELECT id, 'CITIZEN', id, first_name || ' ' || last_name
FROM person
WHERE id IN (
    SELECT resolved_by FROM decision
)
ON CONFLICT DO NOTHING;

-- convert all missing references to system user
UPDATE decision
SET resolved_by = '00000000-0000-0000-0000-000000000000'
WHERE
    decision.resolved_by IS NOT NULL
    AND NOT EXISTS (
        SELECT 1
        FROM evaka_user
        WHERE id = decision.resolved_by
    );

-- add evaka_user constraint
ALTER TABLE decision
    ADD CONSTRAINT fk$resolved_by FOREIGN KEY (resolved_by) REFERENCES evaka_user (id);
