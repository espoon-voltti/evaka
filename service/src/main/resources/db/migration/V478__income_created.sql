ALTER TABLE income
    ADD COLUMN created_at timestamptz,
    ADD COLUMN created_by uuid;

UPDATE income SET created_at = updated_at, created_by = modified_by;

ALTER TABLE income
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN created_by SET NOT NULL;
