#!/bin/bash

# SPDX-FileCopyrightText: 2017-2026 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

# Dumps the canonical database schema from a throwaway, fully isolated Postgres
# container, so the output is canonical regardless of any dev database's state
# (and works with nothing else running). It builds the eVaka test-db image,
# starts a disposable container on a random port, applies migrations with
# `flywayMigrate` (shared db/migration only -- no service, no city afterMigrate
# callbacks), dumps the schema, normalizes it, and tears the container down.
#
# Migrations run via the checked-out working tree's `./gradlew` by default. Set
# SCHEMA_DUMP_MIGRATE_IMAGE to a service-builder image reference to run them
# inside that image instead (CI uses this to reuse the prebuilt builder's warm
# Gradle cache).
#
# The cleaned schema is the only thing written to stdout; all tooling output
# goes to stderr, so `... > docs/db/schema.sql` captures exactly the schema.

set -euo pipefail

BIN_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$BIN_DIR/.." && pwd)"
COMPOSE_DIR="$ROOT_DIR/compose"

TEST_DB_TAG="${TEST_DB_TAG:-master}"
IMAGE="ghcr.io/espoon-voltti/evaka/test-db:${TEST_DB_TAG}"
CONTAINER="evaka-schema-dump-$$"

cleanup() {
  docker rm -f "$CONTAINER" >/dev/null 2>&1 || true
}
trap cleanup EXIT

# Build the test-db image so roles/init match the dev + CI database exactly.
(cd "$COMPOSE_DIR" && docker compose build db) >&2

# Start a disposable container on a random host port (never collides with the
# dev DB on 5432 or with parallel mise instances). init_evaka.sh creates the
# roles + evaka_local/evaka_it on first boot.
docker run -d --name "$CONTAINER" \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e EVAKA_DATABASES=evaka \
  -p 127.0.0.1::5432 \
  "$IMAGE" >/dev/null

# Wait until the real server is accepting TCP connections AND evaka_local exists.
# Probe over TCP (-h 127.0.0.1), not the unix socket: Postgres's transient
# init-phase server listens on the socket only, so a socket probe can pass
# before the real TCP listener (which gradle/flywayMigrate then uses) is bound.
# A successful TCP SELECT against evaka_local proves both that the database
# exists and that the TCP endpoint is ready.
ready=0
for _ in $(seq 1 60); do
  if docker exec -e PGPASSWORD=postgres "$CONTAINER" psql -h 127.0.0.1 -p 5432 -U postgres -d evaka_local -tAc 'SELECT 1' >/dev/null 2>&1; then
    ready=1
    break
  fi
  sleep 1
done
if [ "$ready" -ne 1 ]; then
  echo "evaka-schema-dump: database did not become ready in time" >&2
  exit 1
fi

# Resolve the random host port docker assigned (e.g. "127.0.0.1:49153" -> 49153).
HOST_PORT="$(docker port "$CONTAINER" 5432 | head -1 | sed 's/.*://')"

# Apply migrations with the project's own pinned Flyway (shared db/migration
# locations only); override just the connection URL to the disposable DB.
FLYWAY_URL="jdbc:postgresql://127.0.0.1:${HOST_PORT}/evaka_local"
if [ -n "${SCHEMA_DUMP_MIGRATE_IMAGE:-}" ]; then
  # Run migrations inside the prebuilt builder image (warm Gradle cache;
  # migrations baked in at that commit). --network=host lets the container
  # reach the disposable DB on 127.0.0.1:${HOST_PORT}.
  docker run --rm --network=host "$SCHEMA_DUMP_MIGRATE_IMAGE" \
    ./gradlew --no-daemon -Dorg.gradle.configuration-cache=false \
    flywayMigrate -Pflyway.url="$FLYWAY_URL" >&2
else
  (cd "$ROOT_DIR/service" && ./gradlew --no-daemon -Dorg.gradle.configuration-cache=false \
    flywayMigrate -Pflyway.url="$FLYWAY_URL") >&2
fi

# Dump the schema and normalize it into the canonical docs/db/schema.sql form:
# strip psql meta-commands, dump-header comments, SET statements, and ownership
# suffixes, drop the bodies of the lock_database_nowait / reset_database helper
# functions, then collapse repeated blank lines and trim leading blanks.
# stdout = the only payload.
docker exec -u postgres "$CONTAINER" \
  pg_dump --schema-only --no-owner --no-privileges --no-comments evaka_local \
  | sed '/^\\restrict/d; /^\\unrestrict/d; /^--$/d; /^-- PostgreSQL database dump/d; /^-- Dumped from/d; /^-- Dumped by/d; /^SET /d; /^SELECT pg_catalog/d; s/; Owner: -$//' \
  | awk '
    /-- Name: (lock_database_nowait|reset_database)\(\); Type: FUNCTION/ { skip=1 }
    skip { if (/\$\$;/) skip=0; next }
    { print }
  ' \
  | cat -s \
  | sed '/./,$!d'
