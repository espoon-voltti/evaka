#!/bin/bash

# SPDX-FileCopyrightText: 2017-2020 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

set -eu

cd "$( dirname "${BASH_SOURCE[0]}")"

aws --profile voltti-local s3 sync s3://evaka-deployment-local/frontend/vendor/fortawesome/ ./vendor/fortawesome/
