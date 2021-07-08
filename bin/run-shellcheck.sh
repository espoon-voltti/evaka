#!/bin/sh

# SPDX-FileCopyrightText: 2017-2021 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

# Run shellcheck for scripts
#
# Usage: ./run-shellcheck.sh [path_to_shellcheck_binary]

set -eu

DEBUG=${DEBUG:-false}
if [ "$DEBUG" = "true" ]; then
  set -x
fi

# In CI, the bin can be in a different location
SHELLCHECK_BIN="${1:-shellcheck}"

git ls-files "*.sh" "*.bash" | xargs "$SHELLCHECK_BIN" --external-sources
