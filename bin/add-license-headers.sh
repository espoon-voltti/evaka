#!/usr/bin/env bash

# SPDX-FileCopyrightText: 2017-2020 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

set -euo pipefail

# Configuration
REUSE_YEARS=${REUSE_YEARS:-"2017-$(date +"%Y")"}

if [ "${DEBUG:-false}" = "true" ]; then
    set -x
fi

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

function addheader() {
    local files=("$@")
    reuse annotate --license "LGPL-2.1-or-later" --copyright "City of Espoo" --year "$REUSE_YEARS" "${files[@]}"
}

function handle_missing_licenses() {
    local reuse_output="$1"
    
    # shellcheck disable=SC2207
    local missing_licenses=($(echo "$reuse_output" | grep -o 'Missing licenses: .*' | cut -d ' ' -f 3- | tr ',' ' '))

    # Figure out absolute path to git repository root
    local repo_root
    repo_root=$(git rev-parse --show-superproject-working-tree) # first, let's assume the working directory is in a git submodule
    if [ -z "${repo_root}" ]; then
        # not in a submodule -> just get the repository root
        repo_root=$(git rev-parse --show-toplevel)
    fi
    
    if [ ${#missing_licenses[@]} -gt 0 ]; then
        echo "Downloading missing licenses: ${missing_licenses[*]}"
        for license in "${missing_licenses[@]}"; do
            if [ -n "$license" ] && [ "$license" != "0" ]; then
                if [ ! -f "${repo_root}/LICENSES/${license}.txt" ]; then
                    reuse download "$license"
                fi
            fi
        done
    fi
}

function after_lint() {
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

# Run reuse lint and capture output
set +e
if [ ${#FILES[@]} -gt 0 ]; then
    REUSE_OUTPUT=$(reuse lint-file "${FILES[@]}" 2>&1)
    after_lint
    NONCOMPLIANT_FILES=$(echo "$REUSE_OUTPUT" | cut -d':' -f1)
else
    echo "Checking all files in repository"
    REUSE_OUTPUT=$(reuse lint 2>&1)
    after_lint
    handle_missing_licenses "$REUSE_OUTPUT"
    NONCOMPLIANT_FILES=$(echo "$REUSE_OUTPUT" \
        | awk '/following/{ f = 1; next } /SUMMARY/{ f = 0 } f' \
        | grep -v -e '^$' \
        | cut -d' ' -f2-)
fi

# Parse non-compliant files from output and add headers
if [ -n "$NONCOMPLIANT_FILES" ]; then
    FILES=()
    while IFS= read -r line; do
        if [ -n "$line" ]; then  # Skip empty lines
            FILES+=("$line")
        fi
    done <<< "$NONCOMPLIANT_FILES"
    addheader "${FILES[@]}"
else
    echo "No files need license headers"
fi

echo 'All files are now REUSE compliant'