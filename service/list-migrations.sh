#!/bin/bash -e

# SPDX-FileCopyrightText: 2017-2020 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

ROOT="$(cd "$(dirname "$0")" && pwd)"
MIGRATION_FILES_PATH="$ROOT/src/main/resources/db/migration"
OUT_FILE="$ROOT/src/main/resources/migrations.txt"

"$ROOT"/generate-migration-entries.sh "$MIGRATION_FILES_PATH" > "$OUT_FILE"
git add "$OUT_FILE"
