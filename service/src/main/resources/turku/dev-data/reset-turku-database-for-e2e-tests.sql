-- SPDX-FileCopyrightText: 2021 City of Turku
--
-- SPDX-License-Identifier: LGPL-2.1-or-later

CREATE OR REPLACE FUNCTION reset_turku_database_for_e2e_tests() RETURNS void AS $$
BEGIN
EXECUTE (
    SELECT 'TRUNCATE TABLE ' || string_agg(quote_ident(table_name), ', ') || ' CASCADE'
    FROM information_schema.tables
    WHERE table_schema = 'public'
      AND table_type = 'BASE TABLE'
      AND table_name not in (
        'flyway_schema_history',
        'assistance_action_option',
        'assistance_basis_option',
        'service_need_option',
        'club_term',
        'preschool_term',
        'voucher_value'
    )
);
EXECUTE (
    SELECT 'SELECT ' || string_agg(format('setval(%L, %L, false)', sequence_name, start_value), ', ')
    FROM information_schema.sequences
    WHERE sequence_schema = 'public'
);
END $$ LANGUAGE plpgsql;