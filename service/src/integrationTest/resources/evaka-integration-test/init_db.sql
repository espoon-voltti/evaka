-- SPDX-FileCopyrightText: 2017-2020 City of Espoo
--
-- SPDX-License-Identifier: LGPL-2.1-or-later

-- Idempotent/repeatable integration test database setup

CREATE OR REPLACE FUNCTION reset_database() RETURNS void AS $$
DECLARE
  sequence text;
BEGIN
  EXECUTE (
    SELECT 'TRUNCATE TABLE ' || string_agg(quote_ident(table_name), ', ') || ' CASCADE'
    FROM information_schema.tables
    WHERE table_schema = 'public'
    AND table_type = 'BASE TABLE'
    AND table_name NOT IN (
      'flyway_schema_history',
      'approval_type',
      'decision_status',
      'language',
      'provider_type'
    )
  );
  FOR sequence IN
    SELECT sequence_name
    FROM information_schema.sequences
    WHERE sequence_schema = 'public'
  LOOP
    EXECUTE format('ALTER SEQUENCE %I RESTART', sequence);
  END LOOP;
END $$ LANGUAGE plpgsql;

SELECT reset_database();
