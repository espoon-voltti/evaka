#!/bin/sh
# SPDX-FileCopyrightText: 2017-2026 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

# Installs Appium, its XCUITest driver and a prebuilt WebDriverAgent into the
# gitignored .appium directory, so that the project's own dependencies stay
# untouched. Needs Xcode with an iOS simulator runtime.

set -eu
cd "$(dirname "$0")/../.."

APPIUM_VERSION=3.8.0
XCUITEST_DRIVER_VERSION=12.15.1
export APPIUM_HOME="$PWD/.appium"

mkdir -p "$APPIUM_HOME"
npm install --prefix "$APPIUM_HOME" --no-audit --no-fund "appium@$APPIUM_VERSION"
appium="$APPIUM_HOME/node_modules/.bin/appium"

if ! "$appium" driver list --installed --json | grep -q "\"xcuitest@$XCUITEST_DRIVER_VERSION\""; then
  "$appium" driver uninstall xcuitest 2>/dev/null || true
  "$appium" driver install "xcuitest@$XCUITEST_DRIVER_VERSION"
fi
"$appium" driver run xcuitest build-wda
