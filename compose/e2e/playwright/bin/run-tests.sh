#!/bin/bash

# SPDX-FileCopyrightText: 2017-2021 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

set -euo pipefail

if [ "${DEBUG:-false}" = "true" ]; then
    set -x
fi

export CI="${CI:-false}"
export FORCE_COLOR=1
export PROXY_URL=${PROXY_URL:-http://localhost:9099}
export DUMMY_IDP_URL=${DUMMY_IDP_URL:-http://localhost:9090}

repo=/repo/frontend
image=/project

# The image was built from the frontend sources of some earlier commit and
# holds their node_modules and browsers. The tests run from that copy with the
# sources replaced by the checked-out ones, so a dependency install is needed
# only when the lockfile or the Yarn configuration differs.
if cmp -s "$repo/yarn.lock" "$image/yarn.lock" && cmp -s "$repo/.yarnrc.yml" "$image/.yarnrc.yml"; then
    install_dependencies=false
else
    install_dependencies=true
fi

rm -rf "$image/src"
cp -a "$repo/src" "$image/src"
cp -a "$repo/package.json" "$repo/yarn.lock" "$repo/.yarnrc.yml" "$repo/tsconfig.base.json" "$repo/tsconfig.json" "$image/"
for dir in patches plugins releases; do
    if [ -d "$repo/.yarn/$dir" ]; then
        cp -a "$repo/.yarn/$dir" "$image/.yarn/"
    fi
done
mkdir -p "$repo/test-results"
rm -rf "$image/test-results"
ln -s "$repo/test-results" "$image/test-results"

cd "$image"
if [ "$install_dependencies" = true ]; then
    yarn install --immutable
    yarn exec playwright install
fi

echo 'INFO: Waiting for compose stack to be up ...'
wait-for-url.sh "${PROXY_URL}/api/dev-api/"
wait-for-url.sh "${DUMMY_IDP_URL}/health" "200"

echo "Running tests ..."

if [ -n "${SHARD:-}" ] && [ -n "${SHARD_TOTAL:-}" ]; then
    yarn e2e --shard="$SHARD/$SHARD_TOTAL"
else
    yarn e2e
fi
