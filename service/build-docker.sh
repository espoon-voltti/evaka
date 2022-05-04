#!/usr/bin/env bash

# SPDX-FileCopyrightText: 2017-2020 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

set -euo pipefail

cd "$( dirname "${BASH_SOURCE[0]}")"

GIT_SHA=$(git rev-parse HEAD)

if [ "${1:-}" = "test" ]; then
    docker build --build-arg commit="$GIT_SHA" --build-arg build=local -f Dockerfile -t "evaka-service-builder" --target builder ..
    docker build -f test.Dockerfile -t "evaka-service-test" ..
    docker run --rm evaka-service-test sh -c "./gradlew --no-daemon dependencyCheckUpdate && ./gradlew --no-daemon dependencyCheckAnalyze"
else
    DOCKER_IMAGE=${DOCKER_IMAGE:-evaka/service}
    DOCKER_TAG=${DOCKER_TAG:-local}
    docker build --build-arg commit="$GIT_SHA" --build-arg build=local -f Dockerfile -t "${DOCKER_IMAGE}" ..
    docker tag "${DOCKER_IMAGE}" "${DOCKER_IMAGE}:${DOCKER_TAG}"
    docker tag "${DOCKER_IMAGE}:${DOCKER_TAG}" "${DOCKER_IMAGE}:${GIT_SHA}"
fi
