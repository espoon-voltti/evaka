#!/usr/bin/env bash

# SPDX-FileCopyrightText: 2017-2021 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

set -euo pipefail

cd "$( dirname "${BASH_SOURCE[0]}")"

docker build -t evaka-base .
docker build -t evaka-yarn -f yarn.Dockerfile .
