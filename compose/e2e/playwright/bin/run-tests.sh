#!/bin/bash

# SPDX-FileCopyrightText: 2017-2021 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

set -euo pipefail

export DEBUG="${DEBUG:-false}"
export CI="${CI:-false}"
export FORCE_COLOR=1
export PROXY_URL=${PROXY_URL:-http://localhost:9099}
export KEYCLOAK_URL=${KEYCLOAK_URL:-http://localhost:8080}

if [ "${DEBUG}" = "true" ]; then
    set -x
fi

export FORCE_COLOR=1

cd /repo/frontend

echo 'INFO: Waiting for compose stack to be up...'
./wait-for-url.sh "${PROXY_URL}/api/internal/dev-api"
./wait-for-url.sh "${KEYCLOAK_URL}/auth/realms/evaka-customer/account/" "200"

if test -f playwright-filenames.txt; then
    mapfile -t FILENAMES < playwright-filenames.txt
    yarn e2e-ci-playwright "${FILENAMES[@]}"
else
    yarn e2e-ci-playwright "src/e2e-playwright/specs"
fi
