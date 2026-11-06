#!/bin/sh -eu

# SPDX-FileCopyrightText: 2017-2022 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

VERSION=7.2.0

# A VERSION bump fails here until these are replaced with the output of
# `shasum -a 256 vendor/fortawesome/*.tgz`.
shasum -a 256 -c <<EOF
24dc215d2d3743bda46abbe0b73e9d7bf72764c196de89005ae55bb6fbaa444f  vendor/fortawesome/fortawesome-pro-light-svg-icons-${VERSION}.tgz
52d5e49e978a7d05de82a5e20823b8a64bafc41b593852ece495ac66106a2782  vendor/fortawesome/fortawesome-pro-regular-svg-icons-${VERSION}.tgz
056dbccbf6e1f7f977a4b7392eda9df96c3f3076090909c7246af57d45b3e1a0  vendor/fortawesome/fortawesome-pro-solid-svg-icons-${VERSION}.tgz
EOF

unpack() {
  NAME=${1}
  mkdir -p node_modules/@fortawesome/"${NAME}"
  tar xz --strip-components 1 -C node_modules/@fortawesome/"${NAME}" -f vendor/fortawesome/fortawesome-"${NAME}"-"${VERSION}".tgz
}

unpack pro-light-svg-icons
unpack pro-regular-svg-icons
unpack pro-solid-svg-icons
