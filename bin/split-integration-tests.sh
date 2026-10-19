#!/bin/bash

# SPDX-FileCopyrightText: 2017-2022 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

# Split tests to chunks of roughly equal weight. The weight of a test file is its
# number of @Test annotations, which correlates well with its run time.

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

if [[ ! "$SPLIT_NUMBER" =~ ^[0-9]+$ ]] || [[ ! "$SPLIT_MAX" =~ ^[0-9]+$ ]]; then
    echo "Invalid arguments. Both must be integers"
    exit 1
fi

if (( SPLIT_NUMBER <= 0 )) || (( SPLIT_NUMBER > 20 )) || (( SPLIT_MAX <= 0 )) || (( SPLIT_MAX > 20 )) || (( SPLIT_NUMBER > SPLIT_MAX )); then
    echo "Invalid arguments. Must be between 1 and 20 and number cannot be higher than max"
    exit 1
fi

cd "$(dirname "${BASH_SOURCE[0]}")/../service"

export LC_ALL=C

# Longest-processing-time-first: heaviest files first, each to the lightest chunk
find src/integrationTest/kotlin/evaka/ -type f \( -iname '*Test.kt' -o -iname '*Tests.kt' \) \
    | while read -r file; do
        weight=$(grep -c '@Test' "$file" || true)
        printf '%s\t%s\n' "$(( weight > 0 ? weight : 1 ))" "$file"
    done \
    | sort -t$'\t' -k1,1nr -k2,2 \
    | awk -F'\t' -v chunk="$SPLIT_NUMBER" -v chunks="$SPLIT_MAX" '
        BEGIN { for (i = 1; i <= chunks; i++) total[i] = 0 }
        {
            best = 1
            for (i = 2; i <= chunks; i++) if (total[i] < total[best]) best = i
            total[best] += $1
            if (best == chunk) {
                class = $2
                sub(/^src\/integrationTest\/kotlin\//, "", class)
                sub(/\.kt$/, "", class)
                gsub(/\//, ".", class)
                printf " --tests=%s", class
            }
        }
    '
