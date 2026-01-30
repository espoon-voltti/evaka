#!/bin/bash

# SPDX-FileCopyrightText: 2017-2026 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

set -euo pipefail

DB_NAME="${1:-evaka}_it"

psql -v ON_ERROR_STOP=1 --username "${POSTGRES_USER:-postgres}" <<EOSQL
DROP DATABASE IF EXISTS "$DB_NAME" WITH (FORCE);
CREATE DATABASE "$DB_NAME" OWNER evaka_it;
GRANT ALL PRIVILEGES ON DATABASE "$DB_NAME" TO evaka_migration_role_local WITH GRANT OPTION;
EOSQL

psql -v ON_ERROR_STOP=1 --username "${POSTGRES_USER:-postgres}" --dbname "$DB_NAME" <<EOSQL
GRANT ALL ON SCHEMA public TO evaka_migration_role_local;
EOSQL
