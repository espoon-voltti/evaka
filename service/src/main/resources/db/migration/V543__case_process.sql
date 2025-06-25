ALTER TABLE archived_process RENAME TO case_process;
ALTER TABLE archived_process_history RENAME TO case_process_history;

-- For backwards compatibility during the migration
CREATE VIEW archived_process AS SELECT * FROM case_process;
CREATE VIEW archived_process_history AS SELECT * FROM case_process_history;
