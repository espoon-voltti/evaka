#!/usr/bin/env bash

# SPDX-FileCopyrightText: 2017-2020 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

set -euo pipefail

(cd ../evaka-base && ./build.sh)
(cd ../apigw && ./build-docker.sh)
(cd ../service && ./build-docker.sh)
(cd ../message-service && ./build-docker.sh)
if [ "${ICONS:-X}" = "pro" ]; then
    (cd ../frontend && yarn install --frozen-lockfile)
    (cd ../frontend/packages/enduser-frontend && ICONS=pro yarn build:dev)
    (cd ../frontend/packages/employee-frontend && ICONS=pro yarn build:dev)
    (cd ../frontend/packages/employee-mobile-frontend && ICONS=pro yarn build:dev)
    (cd ../frontend/packages/citizen-frontend && ICONS=pro yarn build:dev)
else
    (cd ../frontend && yarn install --ignore-optional --frozen-lockfile)
    (cd ../frontend/packages/enduser-frontend && yarn build:dev)
    (cd ../frontend/packages/employee-frontend && yarn build:dev)
    (cd ../frontend/packages/employee-mobile-frontend && yarn build:dev)
    (cd ../frontend/packages/citizen-frontend && yarn build:dev)
fi
