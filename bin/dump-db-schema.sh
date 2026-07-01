#!/bin/bash

# SPDX-FileCopyrightText: 2017-2026 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

set -euo pipefail

BIN_DIR="$(cd "$(dirname "$0")" && pwd)"
COMPOSE_DIR="$(cd "$BIN_DIR/../compose" && pwd)"

cd "$COMPOSE_DIR"
docker compose exec -T -u postgres db \
  pg_dump --schema-only --no-owner --no-privileges --no-comments evaka_local \
  | "$BIN_DIR/clean-schema-dump.sh"
