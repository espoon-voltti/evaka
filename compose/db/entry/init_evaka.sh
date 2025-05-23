#!/bin/bash

# SPDX-FileCopyrightText: 2017-2020 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

function create_database() {
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DATABASE" <<EOSQL
        CREATE DATABASE "$1_local";
        CREATE DATABASE "$1_it" OWNER evaka_it;

        GRANT ALL PRIVILEGES ON DATABASE "$1_local" TO "evaka_migration_role_local" WITH GRANT OPTION;
        GRANT ALL PRIVILEGES ON DATABASE "$1_it" TO "evaka_migration_role_local" WITH GRANT OPTION;
EOSQL

    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$1_local" <<EOSQL
        GRANT ALL ON SCHEMA "public" TO "evaka_migration_role_local";

        -- DevDataInitializer creates a few helper functions
        GRANT CREATE ON SCHEMA "public" TO "evaka_application_local";
EOSQL

    PGPASSWORD=flyway psql -v ON_ERROR_STOP=1 --username evaka_migration_local --dbname "$1_local" <<EOSQL
        -- The reset_database function, used in e2e tests, truncates tables and resets sequences
        ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT TRUNCATE ON TABLES TO "evaka_application_local";
        ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT UPDATE ON SEQUENCES TO "evaka_application_local";
EOSQL

    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$1_it" <<EOSQL
        GRANT ALL ON SCHEMA "public" TO "evaka_migration_role_local";
EOSQL
}

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DATABASE" <<EOSQL
    CREATE ROLE evaka_it WITH SUPERUSER LOGIN PASSWORD 'evaka_it';

    -- Migration role to manage the migrations.
    CREATE ROLE "evaka_migration_role_local";

    -- (App) user-level role to connect to the database with least required privileges.
    CREATE ROLE "evaka_application_role_local";

    -- Migration login user
    CREATE ROLE "evaka_migration_local" WITH LOGIN PASSWORD 'flyway'
      IN ROLE "evaka_migration_role_local";

    -- App login user
    CREATE ROLE "evaka_application_local" WITH LOGIN PASSWORD 'app'
      IN ROLE "evaka_application_role_local";
EOSQL

databases=${EVAKA_DATABASES:-evaka}
for database in ${databases//,/ }
do
    create_database "$database"
done
