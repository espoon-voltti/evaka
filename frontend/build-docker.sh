#!/bin/bash

# SPDX-FileCopyrightText: 2017-2022 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

set -euo pipefail

cd "$( dirname "${BASH_SOURCE[0]}")"

rm ./espoo-customizations -rf
cp ../espoo-customizations . -r

if [ "${1:-}" = "test" ]; then
    docker build -t evaka/frontend-builder \
        --target=builder \
        --build-arg build=0 \
        --build-arg commit="$(git rev-parse HEAD)" \
        -f Dockerfile .

    docker run --rm evaka/frontend-builder:latest yarn lint
    docker run --rm evaka/frontend-builder:latest yarn type-check
    docker run --rm evaka/frontend-builder:latest yarn test --maxWorkers=2
else
    docker build -t evaka/frontend \
        --build-arg build=0 \
        --build-arg commit="$(git rev-parse HEAD)" \
        -f Dockerfile .
fi

rm ./espoo-customizations -r
