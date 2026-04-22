#!/bin/sh

# SPDX-FileCopyrightText: 2017-2026 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

set -eu

muni="$1"
if [ -z "$muni" ]; then
  echo 'ERROR: Requires a municipality name'
  exit 1
fi

out="/out/static/$muni"
mkdir -p "$out/employee/mobile"

cp dist/bundle/favicon.ico "$out/"
cp -r dist/bundle/assets "$out/"
cp -r dist/bundle/employee/. "$out/employee/"
cp dist/bundle/src/citizen-frontend/index.html "$out/"
cp dist/bundle/src/employee-frontend/index.html "$out/employee/"
cp dist/bundle/src/employee-mobile-frontend/index.html "$out/employee/mobile/"
cp -r src/maintenance-page-frontend "$out/maintenance-page"
