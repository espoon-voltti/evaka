#!/bin/bash

# SPDX-FileCopyrightText: 2017-2021 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"

if [ "${CI:-false}" = "false" ]; then
    ./build.sh
fi

# shellcheck disable=SC2016
environment_arguments=(
    -e BASIC_AUTH_CREDENTIALS='smoketest:$apr1$m0p2wy4c$OcpUTIZ4za1mRVxt6DuEs/' \
    -e BASIC_AUTH_ENABLED="true" \
    -e ENDUSER_GW_URL="http://fake.test" \
    -e HOST_IP="smoketest" \
    -e INTERNAL_GW_URL="http://fake.test" \
    -e KEYCLOAK_URL="http://fake.test" \
    -e RATE_LIMIT_CIDR_WHITELIST='10.0.0.0/8;192.168.0.0/16' \
    -e SECURITYTXT_CONTACTS="mailto:fake@fake.test;mailto:another@fake.test" \
    -e SECURITYTXT_LANGUAGES="fi,se,en" \
    -e STATIC_FILES_ENDPOINT_URL="http://fake.test" \
)

docker run \
    "${environment_arguments[@]}" \
    -t evaka/proxy \
    nginx -c /etc/nginx/nginx.conf -t

environment_arguments+=(
    -e DD_PROFILING_ENABLED=true
    -e DD_AGENT_HOST=localhost
    -e DD_AGENT_PORT=8126
)
docker run \
    "${environment_arguments[@]}" \
    -t evaka/proxy \
    nginx -c /etc/nginx/nginx.conf -t
