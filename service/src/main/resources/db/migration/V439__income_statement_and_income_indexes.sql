DROP INDEX idx$income_statement_handler_id_null;
CREATE INDEX idx$income_statement_created_handler_id_null ON income_statement (created) WHERE handler_id IS NULL;
CREATE INDEX idx$income_statement_start_date_handler_id_null ON income_statement (start_date) WHERE handler_id IS NULL;
CREATE INDEX idx$income_valid_to_effect_not_incomplete ON income (person_id, valid_to DESC) WHERE effect <> 'INCOMPLETE';
