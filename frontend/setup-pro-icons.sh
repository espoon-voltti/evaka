#!/bin/bash

# SPDX-FileCopyrightText: 2017-2023 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

# Install fortawesome pro icons.

set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"

VERSION="6.4.0"

store_npm() {
  name="$1"
  npm pack "@fortawesome/${name}@${VERSION}"
  mkdir -p "node_modules/@fortawesome/${name}"
  tar xz --strip-components 1 -C "node_modules/@fortawesome/${name}" -f "fortawesome-${name}-${VERSION}.tgz"
  rm "fortawesome-${name}-${VERSION}.tgz"
}

if [ "${FORCE:-false}" = "false" ] && ! test -f .npmrc; then
  echo "Configure .npmrc first. See frontend/README.md for details"
  exit 1
fi

store_npm pro-light-svg-icons
store_npm pro-regular-svg-icons
store_npm pro-solid-svg-icons
