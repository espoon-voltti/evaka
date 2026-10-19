#!/bin/bash

# SPDX-FileCopyrightText: 2017-2026 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

# Print GitHub step outputs about the layer of service/Dockerfile that runs resolveDependencies:
#
#   hash     A hash of the files that the Dockerfile copies before that layer.
#   changed  false if these files and the Dockerfile are the same as in the reference commit, and
#            the reference commit was made in the given week. The registry cache then very likely
#            has the layer, because CI built the reference commit with the same inputs. Otherwise
#            true, also if the reference commit cannot be fetched.
#
# Usage:
#   ./service-deps-layer-inputs.sh week [reference]
#
# week is the CACHE_BUST build argument, which is a part of the cache key of the layer.
# reference is a branch or commit in the origin remote. If it is not given, changed is true.

set -euo pipefail

if [ "$#" -lt 1 ] || [ "$#" -gt 2 ]; then
    echo "Usage: $0 week [reference]" >&2
    exit 1
fi

WEEK="$1"
REFERENCE="${2:-}"

cd "$(dirname "${BASH_SOURCE[0]}")/.."

# Like a COPY source, a glob pathspec does not let * match a slash
INPUT_LIST="$(awk '
    /^COPY / {
        for (i = 2; i < NF; i++) {
            if ($i !~ /^--/) { sub(/^\.\//, "", $i); print ":(glob)" $i }
        }
    }
    /^[^#]*resolveDependencies/ { found = 1; exit }
    END {
        if (!found) { print "No resolveDependencies in service/Dockerfile" > "/dev/stderr"; exit 1 }
    }
' service/Dockerfile)"
mapfile -t INPUTS <<< "$INPUT_LIST"

CHANGED=true
if [ -n "$REFERENCE" ] \
    && git fetch --no-tags --depth=1 origin "$REFERENCE" >&2 \
    && [ "$(TZ=UTC git show -s --format=%cd --date=format-local:%Y-%V FETCH_HEAD)" = "$WEEK" ] \
    && git diff --quiet FETCH_HEAD HEAD -- service/Dockerfile "${INPUTS[@]}"; then
    CHANGED=false
fi

echo "hash=$(git ls-files -s -- "${INPUTS[@]}" | sha256sum | cut -d ' ' -f 1)"
echo "changed=$CHANGED"
