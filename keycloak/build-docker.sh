#!/usr/bin/env bash

# SPDX-FileCopyrightText: 2017-2020 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

set -euo pipefail

cd "$( dirname "${BASH_SOURCE[0]}")"

DOCKER_IMAGE=${DOCKER_IMAGE:-evaka/keycloak}
DOCKER_TAG=${DOCKER_TAG:-local}

docker build -t "${DOCKER_IMAGE}" .

docker tag "${DOCKER_IMAGE}" "${DOCKER_IMAGE}:${DOCKER_TAG}"
