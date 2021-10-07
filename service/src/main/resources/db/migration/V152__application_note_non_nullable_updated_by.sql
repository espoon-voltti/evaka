UPDATE application_note SET updated_by = created_by WHERE updated_by IS NULL;
ALTER TABLE application_note ALTER COLUMN updated_by SET NOT NULL;
