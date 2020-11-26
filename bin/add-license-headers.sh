#!/bin/bash

# SPDX-FileCopyrightText: 2017-2020 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

set -euo pipefail

# Configuration
REUSE_VERSION=0.11.1

if [ "${1:-X}" = '--help' ]; then
  echo 'Usage: ./bin/add-license-headers.sh [OPTIONS]'
  echo -e
  echo 'Helper script to attempt automatically adding missing license headers to all source code files'
  echo 'NOTE: Known non-compliant files are excluded from automatic fixes but not from linting'
  echo -e
  echo 'Options:'
  echo "    --lint-only     Only lint for missing headers, don't attempt to add anything"
  echo '    --help          Print this help'
  exit 0
fi

function addheader() {
    local file="$1"
    shift
    reuse addheader --license "LGPL-2.1-or-later" --copyright "City of Espoo" --year "2017-2020" "$@" "$file"
}

set +e
REUSE_OUTPUT=$(docker run --rm --volume "$(pwd):/data" "fsfe/reuse:${REUSE_VERSION}" lint)
REUSE_EXIT_CODE="$?"
set -e

# No need to continue if everything was OK, or we are just linting
if [ "$REUSE_EXIT_CODE" = 0 ] || [ "${1:-X}" = "--lint-only" ]; then
    echo "$REUSE_OUTPUT"
    exit "$REUSE_EXIT_CODE"
fi

# Linter failures use exit code 1, everything else is unexpected
if [ "$REUSE_EXIT_CODE" != 1 ]; then
    >&2 echo 'ERROR: Unexpected failure during reuse tool execution. Exiting with original exit code!'
    echo "$REUSE_OUTPUT"
    exit "$REUSE_EXIT_CODE"
fi

# Unfortunately reuse tool doesn't provide a machine-readable output currently,
# so some ugly parsing is necessary.
# TODO: Remove excludes when we have reuse-compatible licensing info for them
NONCOMPLIANT_FILES=$(echo "$REUSE_OUTPUT" \
    | awk '/^$/ {next} /following/ {next} /resources\/wsdl/ {next} /espoo-logo/ {next} /MISSING/{flag=1; next} /SUMMARY/{flag=0} flag' \
    | cut -d' ' -f2
)

while IFS= read -r file; do
    if [ -z "$file" ]; then
        continue
    fi

    # reuse tool doesn't currently recognize TSX files
    if [[ "$file" = *tsx ]]; then
        addheader "$file" --style jsx
    else
        addheader "$file"
    fi
done <<< "$NONCOMPLIANT_FILES"

echo 'All files are REUSE compliant, excluding known compliant files'
