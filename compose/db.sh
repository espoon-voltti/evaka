#!/bin/bash

# SPDX-FileCopyrightText: 2017-2021 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"

export GITHUB_REPOSITORY_OWNER="${GITHUB_REPOSITORY_OWNER:-$(git remote get-url origin | sed 's/:/\//g' | rev | cut -d'/' -f2 |rev)}"

backup_path="./backup"
backup_remote_path="/backup"

backup_name="${2:-"evaka-service.dump"}"
dump_local_path="${backup_path}/${backup_name}"
dump_remote_path="${backup_remote_path}/${backup_name}"

#local_postgres_password=postgres # password is set in docker-compose.dbdump.yml environment variables
local_postgres_user=postgres
local_host=db
local_port=5432
local_database=evaka_local

mkdir -p ./backup

if [ "$1" = "dump" ]; then
  docker compose -f docker-compose.yml -f docker-compose.dbdump.yml --rm run dbdump pg_dump -Fc -h "$local_host" -p "$local_port" -U "$local_postgres_user" -f "$dump_remote_path" "$local_database"
  echo "Wrote dump to $dump_remote_path -> $dump_local_path"
elif [ "$1" = "restore" ]; then
  date_string="$(date +%Y%m%d%H%M)"

  docker compose -f docker-compose.yml -f docker-compose.dbdump.yml run --rm -T dbdump psql -h "$local_host" -U "$local_postgres_user" postgres <<EOSQL
    SELECT
        pg_terminate_backend(pid)
    FROM
        pg_stat_activity
    WHERE
        pid <> pg_backend_pid()
        AND datname = '${local_database}';
    ALTER DATABASE "${local_database}" RENAME TO "${local_database}_backup-${date_string}";
EOSQL

  echo "Restoring"
  docker compose -f docker-compose.yml -f docker-compose.dbdump.yml run --rm dbdump pg_restore \
    -Fc \
    --verbose \
    --host="$local_host" \
    --port="$local_port" \
    --username="$local_postgres_user" \
    --dbname="postgres" \
    --no-comments \
    --create \
    "$dump_remote_path"
else
  echo "Usage ./db.sh (dump|restore) [<dump-name>]"
fi
