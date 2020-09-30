#!/bin/sh -e

# SPDX-FileCopyrightText: 2017-2020 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

SCRIPT_ROOT=$(dirname "$0")
MIGRATION_FILES_PATH="$SCRIPT_ROOT/src/main/resources/db/migration"
OUT_FILE="$SCRIPT_ROOT/src/main/resources/migrations.txt"

ORIGINAL=$(cat "$OUT_FILE")
CURRENT=$("$SCRIPT_ROOT"/generate-migration-entries.sh "$MIGRATION_FILES_PATH")

if [ "$ORIGINAL" != "$CURRENT" ]; then
  echo "Please update migrations.txt. Add list-migrations.sh to your pre-commit hook to do that automatically"
  exit 1
fi
