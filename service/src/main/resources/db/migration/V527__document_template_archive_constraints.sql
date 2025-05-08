-- Update any rows that would violate the new constraint
UPDATE document_template
SET archive_externally = false
WHERE archive_externally = true
  AND (process_definition_number IS NULL OR archive_duration_months IS NULL);

-- Constraint to ensure that when archive_externally is true, both process_definition_number and archive_duration_months are set
ALTER TABLE document_template
    ADD CONSTRAINT check$archive_externally_requires_metadata
        CHECK (
            NOT archive_externally OR 
            (process_definition_number IS NOT NULL AND archive_duration_months IS NOT NULL)
        ); 