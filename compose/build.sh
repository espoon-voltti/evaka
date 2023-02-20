#!/usr/bin/env bash

# SPDX-FileCopyrightText: 2017-2020 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

# shellcheck disable=SC2155

set -euo pipefail

(cd ../evaka-base && ./build.sh)
(cd ../apigw && ./build-docker.sh)
(cd ../service && ./build-docker.sh)
(cd ../keycloak && ./build-docker.sh)
(cd ../frontend && ./build-docker.sh)
