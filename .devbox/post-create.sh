#!/bin/bash

# SPDX-FileCopyrightText: 2017-2026 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

set -euo pipefail

curl https://mise.run/bash | bash
export PATH="$HOME/.local/bin:$PATH"
mise trust
mise install
eval "$(mise activate bash)"
mise run install-prompt

(cd frontend && yarn && yarn exec playwright install --with-deps chromium)
