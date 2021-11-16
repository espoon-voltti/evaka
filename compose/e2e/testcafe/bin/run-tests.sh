#!/bin/bash

# SPDX-FileCopyrightText: 2017-2021 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

set -euo pipefail

export DEBUG="${DEBUG:-false}"
export CI="${CI:-false}"
export FORCE_COLOR=1
export PROXY_URL=${PROXY_URL:-http://evaka-proxy:8080}
export KEYCLOAK_URL=${KEYCLOAK_URL:-http://keycloak:8080}

if [ "${DEBUG}" = "true" ]; then
    set -x
fi

cd /repo/frontend

echo 'INFO: Waiting for compose stack to be up...'
./wait-for-url.sh "${PROXY_URL}/api/internal/dev-api"
./wait-for-url.sh "${KEYCLOAK_URL}/auth/realms/evaka-customer/account/" "200"

command=(yarn e2e-ci-testcafe)
if test -f testcafe-fixture-regex.txt; then
    command+=(--fixture-grep "$(cat testcafe-fixture-regex.txt)")
fi
command+=(-- src/e2e-test/specs/)

"${command[@]}"
