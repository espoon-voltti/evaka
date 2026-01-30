#!/bin/bash

# SPDX-FileCopyrightText: 2017-2020 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

function create_database() {
    reset-local-db "$1"
    reset-it-db "$1"
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
