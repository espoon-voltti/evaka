#!/bin/bash

# SPDX-FileCopyrightText: 2017-2020 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

set -euo pipefail

BASEDIR=$(dirname "$0")
PLANTUML_VERSION=1.2020.11

mkdir -p "$BASEDIR"/diagrams/svg
rm -rf "$BASEDIR"/svg/*

for FILE in "$BASEDIR"/diagrams/src/*.puml; do
  FILE_SVG="$BASEDIR"/diagrams/svg/"$(basename "$FILE" | sed 's/puml/svg/')"
  echo Converting "${FILE} -> ${FILE_SVG}"
  docker run --rm -i think/plantuml:"$PLANTUML_VERSION" > "$FILE_SVG" < "$FILE"
done
