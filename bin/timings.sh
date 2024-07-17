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
    run_id="$(gh run list -w build.yml --json headBranch,status,conclusion,databaseId --jq '.[] | select(.status == "completed" and .conclusion == "success" and .headBranch == "master")' -L 100 | sed '1!d' | jq -r .databaseId)"
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
    regex="(src/e2e-test/specs.*) \(([0-9]+\.[0-9]+) s\)"
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

# Sort files by time and put $lines array
lines=($(
    for key in "${!timings[@]}"; do
        echo "${timings[$key]} $key"
    done | sort -n | cut -d ' ' -f 2
))

# Poor man algorithm to split test to even chunks by test execution duration
# Takes first and last element from list sorted by duration
current_split=1
while true; do
    if [[ "${#lines[@]}" = "0" ]]; then
        break
    fi
    if [ "$SPLIT_PRINT" = "$current_split" ]; then
        echo "${lines[0]}"
    fi
    lines=("${lines[@]:1}")
    if [[ "${#lines[@]}" = "0" ]]; then
        break
    fi
    if [ "$SPLIT_PRINT" = "$current_split" ]; then
        echo "${lines[-1]}"
    fi
    unset 'lines[-1]'

    current_split=$((current_split+1))
    if [ "$current_split" = "$(( SPLIT_COUNT + 1 ))" ]; then
        current_split=1
    fi
done
