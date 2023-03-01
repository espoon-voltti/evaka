#!/bin/bash

# SPDX-FileCopyrightText: 2017-2023 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

# Simple test script for timings.sh

# shellcheck disable=SC2034,SC2207

set -euo pipefail

cd "$( dirname "${BASH_SOURCE[0]}")/.."

test_outputs() {
    local -n test_files_1=$1
    local -n test_files_2=$2

    if [ "${#test_files_1[@]}" = "0" ] || [ "${#test_files_1[@]}" = "1" ]; then
        echo "$3: Unexpected lenght: ${#test_files_1[@]}"
        exit 1
    fi
    if [ "${#test_files_1[@]}" != "${#test_files_2[@]}" ]; then
        echo "$3: File outputs are different: ${#test_files_1[@]} vs. ${#test_files_2[@]}"
        exit 1
    fi

    if [ "$(echo "${test_files_1[@]}" "${test_files_2[@]}" | tr ' ' '\n' | sort | uniq -u)" != "" ]; then
        echo "$3: Unexpected elements"
        exit 1
    fi

    echo "$3: ${#test_files_1[@]} vs. ${#test_files_2[@]} - OK"
}

rm -f /tmp/gha-run-cache.txt
export CACHE_RUN="true"

found_test_files=($(cd frontend; find src/e2e-test/specs/ -type f -name '*.ts' | sort ; cd ..))

test_1_files=($(./bin/timings.sh 1 2 && ./bin/timings.sh 2 2 | sort))

test_outputs found_test_files test_1_files "test 1"

test_2_files=($(./bin/timings.sh 2 2 && ./bin/timings.sh 1 2 | sort))

test_outputs found_test_files test_2_files "test 2"

test_3_files=($(./bin/timings.sh 1 5 && ./bin/timings.sh 2 5 && ./bin/timings.sh 3 5 && ./bin/timings.sh 4 5 && ./bin/timings.sh 5 5 | sort))

test_outputs found_test_files test_3_files "test 3"

test_4_files=($(./bin/timings.sh 1 1 | sort))

test_outputs found_test_files test_4_files "test 4"
