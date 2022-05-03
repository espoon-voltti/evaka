#!/bin/bash

# SPDX-FileCopyrightText: 2017-2021 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

# Run shellcheck for scripts
#
# Usage: ./run-shellcheck.sh

set -eu

if [ "${DEBUG:-false}" = "true" ]; then
  set -x
fi

cd "$( dirname "${BASH_SOURCE[0]}")"

git ls-files "*.sh" "*.bash" | xargs docker run --rm -v "$(pwd):/mnt" koalaman/shellcheck:stable --external-sources
