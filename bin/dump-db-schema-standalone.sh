#!/bin/bash

# SPDX-FileCopyrightText: 2017-2026 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

# Dumps the canonical database schema from a throwaway, fully isolated
# Postgres container. Unlike dump-db-schema.sh it does not touch the dev
# environment's database: it builds the eVaka test-db image, starts a
# disposable container on a random port, runs `flywayMigrate` against it
# (shared db/migration only -- no service, no city afterMigrate callbacks),
# dumps through the shared cleaner, and tears the container down. The cleaned
# schema is the only thing written to stdout; all tooling output goes to stderr.

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
(cd "$ROOT_DIR/service" && ./gradlew --no-daemon -Dorg.gradle.configuration-cache=false \
  flywayMigrate -Pflyway.url="jdbc:postgresql://127.0.0.1:${HOST_PORT}/evaka_local") >&2

# Dump and clean (stdout = the only payload).
docker exec -u postgres "$CONTAINER" \
  pg_dump --schema-only --no-owner --no-privileges --no-comments evaka_local \
  | "$BIN_DIR/clean-schema-dump.sh"
