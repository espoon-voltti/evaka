ALTER TABLE absence ADD COLUMN modified_by uuid CONSTRAINT fk$modified_by REFERENCES evaka_user (id);

UPDATE absence SET modified_by = CASE
    WHEN modified_by_employee_id IS NOT NULL THEN modified_by_employee_id
    WHEN modified_by_guardian_id IS NOT NULL THEN modified_by_guardian_id
    WHEN modified_by_deprecated IS NOT NULL THEN (SELECT id FROM evaka_user WHERE type = 'UNKNOWN' AND name = modified_by_deprecated)
    ELSE '00000000-0000-0000-0000-000000000000'
END;

ALTER TABLE absence
    DROP COLUMN modified_by_employee_id,
    DROP COLUMN modified_by_guardian_id,
    DROP COLUMN modified_by_deprecated,
    ALTER COLUMN modified_by SET NOT NULL;
