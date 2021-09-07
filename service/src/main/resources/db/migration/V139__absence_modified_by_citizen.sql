ALTER TABLE absence ALTER COLUMN modified_by DROP NOT NULL;
ALTER TABLE absence RENAME COLUMN modified_by TO modified_by_deprecated;
ALTER TABLE absence ADD COLUMN modified_by_employee_id uuid REFERENCES employee(id);
ALTER TABLE absence ADD COLUMN modified_by_guardian_id uuid REFERENCES person(id);

UPDATE absence SET modified_by_employee_id = coalesce(
    (SELECT id FROM employee WHERE modified_by_deprecated = id::text),
    (SELECT id FROM employee WHERE modified_by_deprecated = concat(first_name, ' ', last_name) LIMIT 1)
);
UPDATE absence SET modified_by_deprecated = NULL WHERE modified_by_employee_id IS NOT NULL;

ALTER TABLE absence ADD CONSTRAINT modified_by_someone CHECK (num_nonnulls(modified_by_employee_id, modified_by_guardian_id, modified_by_deprecated) = 1);
