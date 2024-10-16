ALTER TABLE document_template
    DROP CONSTRAINT validity_start_not_null,
    ADD CONSTRAINT check$validity CHECK (NOT lower_inf(validity));
