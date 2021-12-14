#!/usr/bin/env bash

# SPDX-FileCopyrightText: 2017-2020 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"

IMAGE="evaka/api-gateway"
BUILD_IMAGE="evaka/api-gateway-build"

docker build -t "$BUILD_IMAGE" \
              --build-arg "BASE_IMAGE=${BASE_IMAGE}" \
              --build-arg "build=${CIRCLE_BUILD_NUM:-0}" \
              --build-arg "commit=${CIRCLE_SHA1:-local}" \
              --target=builder \
              .

docker run --rm -t "$BUILD_IMAGE" yarn lint

docker rm -f dummy || true
docker run --name dummy -t "$BUILD_IMAGE" yarn test-ci

docker cp dummy:/project/build/test-reports "$(pwd)/test-reports"
docker rm -f dummy

docker build -t "$IMAGE" \
              --build-arg "BASE_IMAGE=${BASE_IMAGE}" \
              --build-arg "build=${CIRCLE_BUILD_NUM:-0}" \
              --build-arg "commit=${CIRCLE_SHA1:-local}" \
              .
