#!/bin/bash

set -e

dump_path="${2:-/tmp/evaka-service.dump}"
local_postgres_password=postgres
local_postgres_user=postgres
local_host=localhost
local_port=5432
local_database=evaka_local

if [ "$1" = "dump" ]; then
  PGPASSWORD="$local_postgres_password" pg_dump -Fc -h "$local_host" -p "$local_port" -U "$local_postgres_user" -f "$dump_path" "$local_database"
  echo "Wrote dump to $dump_path"
elif [ "$1" = "restore" ]; then
  date_string="$(date +%Y%m%d%H%M)"

  PGPASSWORD="$local_postgres_password" psql -h "$local_host" -U "$local_postgres_user" postgres <<EOSQL
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
  PGPASSWORD="$local_postgres_password" pg_restore \
    -Fc \
    --verbose \
    --host="$local_host" \
    --port="$local_port" \
    --username="$local_postgres_user" \
    --dbname="postgres" \
    --no-comments \
    --create \
    "$dump_path"
else
  echo "Usage ./db.sh (dump|restore) [<dump-path>]"
fi
