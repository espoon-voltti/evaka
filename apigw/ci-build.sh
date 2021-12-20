#!/usr/bin/env bash

# SPDX-FileCopyrightText: 2017-2020 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"

IMAGE="evaka/api-gateway"
TEST_IMAGE="evaka/api-gateway-test"
BUILD_IMAGE="evaka/api-gateway-build"

docker build -t "$BUILD_IMAGE" \
              --build-arg "BASE_IMAGE=${BASE_IMAGE}" \
              --build-arg "build=${CIRCLE_BUILD_NUM:-0}" \
              --build-arg "commit=${CIRCLE_SHA1:-local}" \
              --target=builder \
              .

docker build -t "$TEST_IMAGE" \
              --build-arg "BASE_IMAGE=${BASE_IMAGE}" \
              --build-arg "build=${CIRCLE_BUILD_NUM:-0}" \
              --build-arg "commit=${CIRCLE_SHA1:-local}" \
              --target=test \
              .

docker build -t "$IMAGE" \
              --build-arg "BASE_IMAGE=${BASE_IMAGE}" \
              --build-arg "build=${CIRCLE_BUILD_NUM:-0}" \
              --build-arg "commit=${CIRCLE_SHA1:-local}" \
              .
