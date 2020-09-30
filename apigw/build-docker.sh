#!/usr/bin/env bash

# SPDX-FileCopyrightText: 2017-2020 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

set -euo pipefail

DOCKER_IMAGE=${DOCKER_IMAGE:-evaka/api-gateway}
DOCKER_TAG=${DOCKER_TAG:-local}
GIT_SHA=$(git rev-parse HEAD)

yarn build

docker build --build-arg commit="${GIT_SHA}" --build-arg build=local -t "${DOCKER_IMAGE}" .
docker tag "${DOCKER_IMAGE}" "${DOCKER_IMAGE}:${DOCKER_TAG}"
docker tag "${DOCKER_IMAGE}:${DOCKER_TAG}" "${DOCKER_IMAGE}:${GIT_SHA}"
