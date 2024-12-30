ALTER TABLE income
    ADD COLUMN created_at timestamptz,
    ADD COLUMN created_by uuid,
    ADD CONSTRAINT fk$created_by FOREIGN KEY (created_by) REFERENCES evaka_user(id);

UPDATE income SET created_at = updated_at, created_by = modified_by;

ALTER TABLE income
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN created_by SET NOT NULL;

CREATE INDEX idx$income_created_by ON income(created_by);
