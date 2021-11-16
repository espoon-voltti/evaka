CREATE INDEX CONCURRENTLY idx$income_statement_handler_id_null ON income_statement(id) WHERE (handler_id IS NULL);
