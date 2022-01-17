#!/bin/bash

# SPDX-FileCopyrightText: 2017-2021 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

set -euo pipefail

export DEBUG="${DEBUG:-false}"
export CI="${CI:-false}"
export FORCE_COLOR=1
export PROXY_URL=${PROXY_URL:-http://evaka-proxy:8080}

if [ "${DEBUG}" = "true" ]; then
    set -x
fi

export FORCE_COLOR=1

cd /repo/frontend

echo 'INFO: Waiting for compose stack to be up...'
./wait-for-dev-api.sh "$PROXY_URL"

mapfile -t FILENAMES < playwright-filenames.txt
yarn e2e-ci "${FILENAMES[@]}"
