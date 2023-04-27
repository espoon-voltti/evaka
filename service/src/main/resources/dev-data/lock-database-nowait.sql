-- SPDX-FileCopyrightText: 2017-2022 City of Espoo
--
-- SPDX-License-Identifier: LGPL-2.1-or-later

CREATE OR REPLACE FUNCTION lock_database_nowait() RETURNS void AS $$
BEGIN
    EXECUTE (
        SELECT 'LOCK TABLE ' || string_agg(quote_ident(table_name), ', ' ORDER BY table_name) || ' NOWAIT'
        FROM information_schema.tables
        WHERE table_schema = 'public'
        AND table_type = 'BASE TABLE'
    );
END $$ LANGUAGE plpgsql;
COMMENT ON FUNCTION lock_database_nowait() IS
    'Obtains a strongest possible lock (ACCESS EXCLUSIVE) on all database tables.
    If locking is not possible without waiting, aborts the current transaction.';
