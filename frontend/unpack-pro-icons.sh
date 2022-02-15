#!/bin/sh -eu

# SPDX-FileCopyrightText: 2017-2022 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

VERSION=6.0.0

unpack() {
  NAME=${1}
  mkdir -p node_modules/@fortawesome/"${NAME}"
  tar xz --strip-components 1 -C node_modules/@fortawesome/"${NAME}" -f vendor/fortawesome/fortawesome-"${NAME}"-"${VERSION}".tgz
}

unpack pro-light-svg-icons
unpack pro-regular-svg-icons
unpack pro-solid-svg-icons
