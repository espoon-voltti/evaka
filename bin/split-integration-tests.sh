#!/bin/bash

# SPDX-FileCopyrightText: 2017-2022 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

# Split tests to chunks

set -euo pipefail

SPLIT_NUMBER="${1:-}"
SPLIT_MAX="${2:-}"

SPLIT_SEED=123

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

cd "$( dirname "${BASH_SOURCE[0]}")/../service"

for test in $(find src/integrationTest/kotlin/fi/espoo/evaka/ -type f \( -iname '*Test.kt' -o -iname '*Tests.kt' \) | sed 's/^src\/integrationTest\/kotlin\///g' | sed 's/.kt$//g' | tr / . | shuf --random-source=<(openssl enc -aes-256-ctr -pass pass:"$SPLIT_SEED" -nosalt </dev/zero 2>/dev/null) | split --suffix-length=1 --numeric-suffixes=1 --number="r/${SPLIT_NUMBER}/${SPLIT_MAX}"); do
    echo -n " --tests=$test"
done
