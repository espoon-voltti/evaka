ALTER TABLE income_statement
    ADD CONSTRAINT check$income_statement_citizen_modified_at
    CHECK ((sent_at IS NULL) = (citizen_modified_at IS NULL));
