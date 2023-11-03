#!/bin/bash

# SPDX-FileCopyrightText: 2017-2020 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

set -euo pipefail

BASEDIR=$(dirname "$0")
TARGETDIR="${BASEDIR}/diagrams/png"
PLANTUML_VERSION=1.2023.12

rm -rf "${TARGETDIR}"
mkdir -p "${TARGETDIR}"

for SRCFILE in "${BASEDIR}"/diagrams/src/*.puml; do
  TARGETFILE="${TARGETDIR}"/$(basename "${SRCFILE}" | sed 's/puml/png/')
  echo Converting "${SRCFILE} -> ${TARGETFILE}"
  docker run --rm -i dstockhammer/plantuml:"${PLANTUML_VERSION}" -pipe > "${TARGETFILE}" < "${SRCFILE}"
done
