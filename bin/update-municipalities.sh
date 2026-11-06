#!/usr/bin/env bash

# SPDX-FileCopyrightText: 2017-2026 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

# Writes the municipality classification of Statistics Finland for the year YEAR to
# service/src/main/resources/vtj/municipalities.csv

set -euo pipefail
export LC_ALL=C

year="${1:?Usage: $0 YEAR}"
url="https://data.stat.fi/api/classifications/v2/classifications/kunta_1_${year}0101/classificationItems?content=data&meta=max&format=json&lang="
out="$(dirname "$0")/../service/src/main/resources/vtj/municipalities.csv"

names() {
    curl -sSf "${url}$1" | jq -r '.[] | [.code, .classificationItemNames[0].name] | join(";")' | sort
}

{
    echo "code;fi;sv"
    join -t ';' <(names "fi") <(names "sv")
} > "$out"
