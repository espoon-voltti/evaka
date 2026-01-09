#!/bin/bash

# SPDX-FileCopyrightText: 2017-2020 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

set -euo pipefail

DB_PORT="${EVAKA_DB_PORT:-5432}"

until nc -z "localhost" "$DB_PORT"; do
  echo "Waiting for PostgreSQL on port $DB_PORT"
  sleep 1
done

exec "$@"
