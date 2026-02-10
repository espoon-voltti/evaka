#!/bin/bash

# SPDX-FileCopyrightText: 2017-2026 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

set -euo pipefail

COMPOSE_DIR="$(cd "$(dirname "$0")/../compose" && pwd)"

cd "$COMPOSE_DIR"
docker compose exec -T -u postgres db \
  pg_dump --schema-only --no-owner --no-privileges --no-comments evaka_local \
  | sed '/^\\restrict/d; /^\\unrestrict/d; /^--$/d; /^-- PostgreSQL database dump/d; /^-- Dumped from/d; /^-- Dumped by/d; /^SET /d; /^SELECT pg_catalog/d; s/; Owner: -$//' \
  | awk '
    /-- Name: (lock_database_nowait|reset_database)\(\); Type: FUNCTION/ { skip=1 }
    skip { if (/\$\$;/) skip=0; next }
    { print }
  ' \
  | cat -s \
  | sed '/./,$!d'
