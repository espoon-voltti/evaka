-- SPDX-FileCopyrightText: 2017-2020 City of Espoo
--
-- SPDX-License-Identifier: LGPL-2.1-or-later

CREATE OR REPLACE FUNCTION reset_database() RETURNS void AS $$
BEGIN
  EXECUTE (
    SELECT 'TRUNCATE TABLE ' || string_agg(quote_ident(table_name), ', ') || ' CASCADE'
    FROM information_schema.tables
    WHERE table_schema = 'public'
    AND table_type = 'BASE TABLE'
    AND table_name <> 'flyway_schema_history'
  );
  EXECUTE (
    SELECT 'SELECT ' || string_agg(format('setval(%L, %L, false)', sequence_name, start_value), ', ')
    FROM information_schema.sequences
    WHERE sequence_schema = 'public'
  );
END $$ LANGUAGE plpgsql;
