ALTER TABLE case_process ADD case_identifier text GENERATED ALWAYS AS (number || '/' || process_definition_number || '/' || year) STORED;
