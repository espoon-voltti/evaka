#!/bin/bash

# SPDX-FileCopyrightText: 2017-2023 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

# shellcheck disable=SC2207

# Split test in even chunks by execution duration
# Fetches and parses durations from github action job logs
#
# Usage:   ./timings <test-number> <max-test>
# Example: ./timings 1 2 # first chunk of total of two chunks
#          ./timings 2 2 # second chunk of total of two chunks

set -euo pipefail
#set -x

SPLIT_PRINT="$1"
SPLIT_COUNT="$2"

cd "$( dirname "${BASH_SOURCE[0]}")/.."

_get_log_output() {
    run_id="$(gh run list -w build.yml --json headBranch,status,databaseId --jq '.[] | select(.status == "completed" and .headBranch == "master")' -L 100 | sed '1!d' | jq -r .databaseId)"
    if test -z "$run_id"; then
        echo "Could not get run ID"
        exit 1
    fi
    gh run view "$run_id" --log
}

get_log_output() {
    if [ "${CACHE_RUN:-false}" = "true" ]; then
        if test -f /tmp/gha-run-cache.txt; then
            cat /tmp/gha-run-cache.txt
        else
            _get_log_output | tee -a /tmp/gha-run-cache.txt
        fi
    else
        _get_log_output
    fi
}

# Find test files and durations from logs and store into $timings
declare -A timings
while IFS="" read -r line || [ -n "$line" ]; do
    regex="(src/e2e-test/specs.*)\s\(([0-9]+\.[0-9]+) s\)"
    if [[ $line =~ $regex ]]; then
        timings["${BASH_REMATCH[1]}"]="${BASH_REMATCH[2]}"
    else
        continue
    fi
done < <( get_log_output | grep '^e2e' | sed -r "s/\x1B\[([0-9]{1,3}(;[0-9]{1,2})?)?[mGK]//g" | grep ' e2e-test ' )


# Set missing test times to zeros
while IFS="" read -r line || [ -n "$line" ]; do
    [ "${timings[$line]+found}" ] || timings["$line"]="0"
done < <(cd frontend; find src/e2e-test/specs/ -type f -name '*.ts' ; cd ..)

for key in "${!timings[@]}"; do
    echo "${timings[$key]} $key"
done | sort -rn | shuf --random-source=<(yes 42) | python ./bin/timings.py "$SPLIT_PRINT" "$SPLIT_COUNT"
