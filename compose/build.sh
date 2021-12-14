#!/usr/bin/env bash

# SPDX-FileCopyrightText: 2017-2021 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

set -euo pipefail

../evaka-base/build.sh
../apigw/build-docker.sh

(cd ../service && ./build-docker.sh)
(cd ../frontend && yarn install --immutable)
(cd ../frontend && yarn build:dev)
