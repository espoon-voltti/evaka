#!/bin/bash

# SPDX-FileCopyrightText: 2017-2026 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

# Install fortawesome pro icons from S3.

set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."

VERSION="7.2.0"

s3_download() {
  aws --profile voltti-evaka-shared s3 cp "s3://evaka-assets/fontawesome-${VERSION}/fortawesome-${1}-${VERSION}.tgz" "vendor/fortawesome/fortawesome-${1}-${VERSION}.tgz"
}

s3_download pro-light-svg-icons
s3_download pro-regular-svg-icons
s3_download pro-solid-svg-icons
./bin/unpack-pro-icons.sh
