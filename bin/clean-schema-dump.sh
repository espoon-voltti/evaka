#!/bin/bash

# SPDX-FileCopyrightText: 2017-2026 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

# Normalizes a raw `pg_dump --schema-only` (on stdin) into the canonical
# docs/db/schema.sql form (on stdout): strips psql meta-commands, dump-header
# comments, SET statements, ownership suffixes, and the bodies of the
# lock_database_nowait / reset_database helper functions, then collapses
# repeated blank lines and trims leading blanks.

set -euo pipefail

sed '/^\\restrict/d; /^\\unrestrict/d; /^--$/d; /^-- PostgreSQL database dump/d; /^-- Dumped from/d; /^-- Dumped by/d; /^SET /d; /^SELECT pg_catalog/d; s/; Owner: -$//' \
  | awk '
    /-- Name: (lock_database_nowait|reset_database)\(\); Type: FUNCTION/ { skip=1 }
    skip { if (/\$\$;/) skip=0; next }
    { print }
  ' \
  | cat -s \
  | sed '/./,$!d'
