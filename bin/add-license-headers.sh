#!/usr/bin/env bash

# SPDX-FileCopyrightText: 2017-2020 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

set -euo pipefail

# Configuration
DEBUG=${DEBUG:-false}
REUSE_VERSION=0.13.0
REUSE_YEARS=${REUSE_YEARS:-"2017-$(date +"%Y")"}

if [ "$DEBUG" = "true" ]; then
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
  echo 'Usage: ./bin/add-license-headers.sh [OPTIONS]'
  echo -e
  echo 'Helper script to attempt automatically adding missing license headers to all source code files.'
  echo 'Any missing license files are downloaded automatically to LICENSES/.'
  echo 'NOTE: Known non-compliant files are excluded from automatic fixes but not from linting.'
  echo -e
  echo 'Options:'
  echo "    --lint-only     Only lint for missing headers, don't attempt to add anything"
  echo '    --help          Print this help'
  exit 0
fi

function run_reuse() {
    run_args=("$@")
    docker run --rm --volume "${REPO_ROOT}:/data" --workdir "/data${REPO_PREFIX}" "fsfe/reuse:${REUSE_VERSION}" "${run_args[@]}"
}

function addheader() {
    local file="$1"
    shift
    local cmd_args=("$@")
    run_reuse addheader --license "LGPL-2.1-or-later" --copyright "City of Espoo" --year "$REUSE_YEARS" "${cmd_args[@]}" "$file"
}

set +e
REUSE_OUTPUT=$(run_reuse lint)
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

# NOTE: All of the following nonsense can be dropped if "reuse json" or something else machine-readable
# is ever implemented in the tool (https://github.com/fsfe/reuse-tool/issues/183).

# If licenses referenced in some file are missing, the output contains:
#
# * Missing licenses: BSD-2-Clause, ANOTHER-LICENSE
#
# -> find all quoted license IDs and download them automatically
# shellcheck disable=SC2207
MISSING_LICENSES=($(echo "$REUSE_OUTPUT" | grep '^* Missing licenses:' | cut -d ' ' -f 4- | tr ', ' ' '))
for license in "${MISSING_LICENSES[@]}"; do
    if [ -z "$license" ]; then
        continue
    fi

    run_reuse download "$license"
done

# Unfortunately reuse tool doesn't provide a machine-readable output currently,
# so some ugly parsing is necessary.
# TODO: Remove excludes when we have reuse-compatible licensing info for them
NONCOMPLIANT_FILES=$(echo "$REUSE_OUTPUT" \
    | awk '/^$/ {next} /following/ {next} /resources\/wsdl/ {next} /espoo-logo/ {next} /EspooLogo/ {next} /MISSING COPYRIGHT AND LICENSING INFORMATION/{flag=1; next} /SUMMARY/{flag=0} flag' \
    | cut -d' ' -f2-
)

while IFS= read -r file; do
    if [ -z "$file" ]; then
        continue
    fi

    addheader "$file"
done <<< "$NONCOMPLIANT_FILES"

echo 'All files are REUSE compliant, excluding known compliant files'
