#!/bin/bash

# SPDX-FileCopyrightText: 2017-2023 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

# Install fortawesome pro icons.

set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"

VERSION="7.2.0"

store_npm() {
  name="$1"
  npm pack "@fortawesome/${name}@${VERSION}"
  mv "fortawesome-${name}-${VERSION}.tgz" vendor/fortawesome/
}

if [ "${FORCE:-false}" = "false" ] && ! test -f .npmrc; then
  echo "Configure .npmrc first. See frontend/README.md for details"
  exit 1
fi

store_npm pro-light-svg-icons
store_npm pro-regular-svg-icons
store_npm pro-solid-svg-icons
./unpack-pro-icons.sh
