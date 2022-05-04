#!/bin/bash

# SPDX-FileCopyrightText: 2017-2022 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

# Split tests to chunks

set -euo pipefail

SPLIT_NUMBER="${1:-}"
SPLIT_MAX="${2:-}"

if test -z "$SPLIT_NUMBER" || test -z "$SPLIT_MAX"; then
    echo "Usage: $0 chunk-number number-of-chunks"
    echo "Example:"
    echo "  $0 1 2"
    echo "  $0 2 2"
    exit 1
fi

if (( SPLIT_NUMBER <= 0 )) || (( SPLIT_NUMBER > 9 )) || (( SPLIT_MAX <= 0 )) || (( SPLIT_MAX > 9 )) || (( SPLIT_NUMBER > SPLIT_MAX )); then
    echo "Invalid arguments. Must be between 1 and 9 and number cannot be higher than max"
    exit 1
fi

cd "$( dirname "${BASH_SOURCE[0]}")/../frontend"

rm -f playwright-filenames.txt

rm -rf /tmp/test-split
mkdir -p /tmp/test-split

trap 'rm -rf /tmp/test-split' EXIT

find src/e2e-test/ -type f -name '*.spec.ts' | sort -h > /tmp/test-split/lines

split --suffix-length=1 --numeric-suffixes=1 --number="l/${SPLIT_MAX}" /tmp/test-split/lines "/tmp/test-split/split."

cat "/tmp/test-split/split.$SPLIT_NUMBER" > playwright-filenames.txt

cat playwright-filenames.txt
