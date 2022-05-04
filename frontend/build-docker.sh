#!/bin/bash

# SPDX-FileCopyrightText: 2017-2022 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

set -euo pipefail

cd "$( dirname "${BASH_SOURCE[0]}")"

rm ./espoo-customizations -rf
cp ../espoo-customizations . -r

docker build -t evaka/frontend \
    --build-arg build=0 \
    --build-arg commit="$(git rev-parse HEAD)" \
    -f Dockerfile .

rm ./espoo-customizations -r
