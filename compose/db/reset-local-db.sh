#!/bin/bash

# SPDX-FileCopyrightText: 2017-2026 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

set -euo pipefail

DB_NAME="${1:-evaka}_local"
PG_USER="${POSTGRES_USER:-postgres}"

psql -v ON_ERROR_STOP=1 --username "$PG_USER" <<EOSQL
DROP DATABASE IF EXISTS "$DB_NAME" WITH (FORCE);
CREATE DATABASE "$DB_NAME";
GRANT ALL PRIVILEGES ON DATABASE "$DB_NAME" TO evaka_migration_role_local WITH GRANT OPTION;
EOSQL

psql -v ON_ERROR_STOP=1 --username "$PG_USER" --dbname "$DB_NAME" <<EOSQL
GRANT ALL ON SCHEMA public TO evaka_migration_role_local;
GRANT CREATE ON SCHEMA public TO evaka_application_local;
EOSQL

PGPASSWORD=flyway psql -v ON_ERROR_STOP=1 --username evaka_migration_local --dbname "$DB_NAME" <<EOSQL
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT TRUNCATE ON TABLES TO evaka_application_local;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT UPDATE ON SEQUENCES TO evaka_application_local;
EOSQL
