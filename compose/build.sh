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
    (cd ../frontend && ICONS=pro yarn install --immutable)
else
    (cd ../frontend && yarn install --immutable)
fi
