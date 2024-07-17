#!/bin/bash

# SPDX-FileCopyrightText: 2017-2024 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

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

# Jest only reports times for test suites that take >5 seconds. Add missing files using 3 seconds as the time.
while IFS="" read -r line || [ -n "$line" ]; do
    [ "${timings[$line]+found}" ] || timings["$line"]="3.0"
done < <(cd frontend; find src/e2e-test/specs -type f -name '*.ts' ; cd ..)

# Sort files by time, longest time first
lines="$(
    for key in "${!timings[@]}"; do
        echo "${timings[$key]} $key"
    done | sort -gr | cut -d ' ' -f 2
)"

# Initialize buckets
declare -A buckets
declare -A bucket_sums
for ((i=0; i<SPLIT_COUNT; i++)); do
    buckets[$i]=""
    bucket_sums[$i]=0
done

for line in $lines; do
    weight="${timings[$line]}"

    # Find the bucket with the smallest sum
    min_index=0
    min_sum=${bucket_sums[0]}
    for ((i=1; i<SPLIT_COUNT; i++)); do
        if [ "$(echo "${bucket_sums[$i]} < $min_sum" | bc -l)" = 1 ] ; then
            min_index=$i
            min_sum=${bucket_sums[$i]}
        fi
    done

    # Add line to that bucket
    if [ -z "${buckets[$min_index]}" ]; then
        buckets[$min_index]="$line"
    else
        buckets[$min_index]="${buckets[$min_index]} $line"
    fi
    bucket_sums[$min_index]=$(echo "${bucket_sums[$min_index]} + $weight" | bc -l)
done

# Print info to stderr
for ((i=0; i<SPLIT_COUNT; i++)); do
    echo "Chunk $((i+1)): ${bucket_sums[$i]} seconds" >&2
    for line in ${buckets[$i]}; do
        echo "  $line - ${timings[$line]} seconds" >&2
    done
done

# Print filenames in the selected bucket to stdout
for line in ${buckets[$((SPLIT_PRINT-1))]}; do
    echo "$line"
done
