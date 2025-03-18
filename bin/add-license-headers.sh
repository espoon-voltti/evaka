#!/usr/bin/env bash

# SPDX-FileCopyrightText: 2017-2020 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

set -euo pipefail

# Configuration
REUSE_VERSION="5"
REUSE_YEARS=${REUSE_YEARS:-"2017-$(date +"%Y")"}

REUSE_IMAGE="fsfe/reuse:${REUSE_VERSION}"

if [ "${DEBUG:-false}" = "true" ]; then
    set -x
fi

# Figure out absolute path to git repository root
REPO_ROOT=$(git rev-parse --show-superproject-working-tree) # first, let's assume the working directory is in a git submodule
if [ -z "${REPO_ROOT}" ]; then
  # not in a submodule -> just get the repository root
  REPO_ROOT=$(git rev-parse --show-toplevel)
fi

# strip absolute git repository path from absolute working directory path
REPO_PREFIX=${PWD#"${REPO_ROOT}"}

if [ "${1:-X}" = '--help' ]; then
  echo 'Usage: ./bin/add-license-headers.sh [OPTIONS] [FILE_PATHS...]'
  echo ''
  echo 'Helper script to attempt automatically adding missing license headers to source code files.'
  echo 'Any missing license files are downloaded automatically to LICENSES/.'
  echo ''
  echo 'Options:'
  echo "    --lint-only     Only lint for missing headers, don't attempt to add anything"
  echo '    --help          Print this help'
  echo ''
  echo 'If FILE_PATHS are provided, only those files will be checked and updated.'
  echo 'Otherwise, all files in the repository will be processed.'
  exit 0
fi

function run_reuse() {
    run_args=("$@")
    docker run -u "${UID}" --rm --volume "${REPO_ROOT}:/data" --workdir "/data${REPO_PREFIX}" "$REUSE_IMAGE" "${run_args[@]}"
}

function addheader() {
    local file="$1"
    run_reuse annotate --license "LGPL-2.1-or-later" --copyright "City of Espoo" --year "$REUSE_YEARS" "$file"
}

# MAIN SCRIPT

# Handle options and file paths
LINT_ONLY=false
FILES=()

for arg in "$@"; do
    if [ "$arg" = "--lint-only" ]; then
        LINT_ONLY=true
    elif [[ "$arg" != --* ]]; then
        FILES+=("$arg")
    fi
done

# If specific files were provided, check only those
if [ ${#FILES[@]} -gt 0 ]; then
    if [ "$LINT_ONLY" = true ]; then
        # Check specific files in lint-only mode
        run_reuse lint-file "${FILES[@]}"
        exit $?
    else
        # Process specific files
        for file in "${FILES[@]}"; do
            if [ -f "$file" ]; then
                # Check if file already has a license header
                if ! run_reuse lint-file "$file" &>/dev/null; then
                    echo "Adding license header to $file"
                    addheader "$file"
                else
                    echo "File already compliant: $file"
                fi
            else
                echo "Warning: File not found: $file"
            fi
        done
        echo "Finished processing specified files"
        exit 0
    fi
fi

# Process all files when no specific files provided
set +e
REUSE_OUTPUT=$(run_reuse lint)
REUSE_EXIT_CODE="$?"
set -e

# No need to continue if everything was OK, or we are just linting
if [ "$REUSE_EXIT_CODE" = 0 ] || [ "$LINT_ONLY" = true ]; then
    echo "OK - $REUSE_OUTPUT"
    exit "$REUSE_EXIT_CODE"
fi

# Linter failures use exit code 1, everything else is unexpected
if [ "$REUSE_EXIT_CODE" != 1 ]; then
    >&2 echo 'ERROR: Unexpected failure during reuse tool execution. Exiting with original exit code!'
    echo "$REUSE_OUTPUT"
    exit "$REUSE_EXIT_CODE"
fi

# NOTE: All of the following nonsense can be dropped if "reuse json" or something else machine-readable
# is ever implemented in the tool (https://github.com/fsfe/reuse-tool/issues/183).

# If licenses referenced in some file are missing, the output contains:
#
# * Missing licenses: BSD-2-Clause, ANOTHER-LICENSE
#
# -> find all quoted license IDs and download them automatically
# shellcheck disable=SC2207
MISSING_LICENSES=($(echo "$REUSE_OUTPUT" | grep '^* Missing licenses:' | cut -d ' ' -f 4- | tr ', ' ' '))
echo "${MISSING_LICENSES[@]}"
for license in "${MISSING_LICENSES[@]}"; do
    if [ -z "$license" ] || [ "$license" = "0" ]; then
        continue
    fi

    if [ ! -f "${REPO_ROOT}/LICENSES/${license}.txt" ]; then
        run_reuse download "$license"
    fi
done

# Unfortunately reuse tool doesn't provide a machine-readable output currently,
# so some ugly parsing is necessary.
NONCOMPLIANT_FILES=$(echo "$REUSE_OUTPUT" \
    | awk '/following/{ f = 1; next } /SUMMARY/{ f = 0 } f' \
    | grep -v -e '^$' \
    | cut -d' ' -f2-
)

while IFS= read -r file; do
    if [ -z "$file" ]; then
        continue
    fi

    addheader "$file"
done <<< "$NONCOMPLIANT_FILES"

echo 'All files are REUSE compliant'