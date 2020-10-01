#!/usr/bin/env bash

# SPDX-FileCopyrightText: 2017-2020 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

ICONS_DIR=$(dirname "${BASH_SOURCE[0]}")
ICON_SET="${1:-X}"

if [ "$ICON_SET" = 'X' ] && [ -f "$ICONS_DIR/package.json" ] && [ -f "$ICONS_DIR/icons.ts" ]; then
  exit 0
fi

if [ "$ICON_SET" = 'pro' ]; then
  echo "Setting up pro version of Font Awesome icons"
  ln -Ff "$ICONS_DIR/pro.icons.ts" "$ICONS_DIR/icons.ts"
  ln -Ff "$ICONS_DIR/pro.package.json" "$ICONS_DIR/package.json"
else
  echo "Setting up free version of Font Awesome icons"
  ln -Ff "$ICONS_DIR/free.icons.ts" "$ICONS_DIR/icons.ts"
  ln -Ff "$ICONS_DIR/free.package.json" "$ICONS_DIR/package.json"
fi
