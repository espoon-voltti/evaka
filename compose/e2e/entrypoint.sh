#!/bin/bash

# SPDX-FileCopyrightText: 2017-2021 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

set -euo pipefail

export DEBUG="${DEBUG:-false}"
export TESTS="${TESTS:-}"

if [ "${DEBUG}" = "true" ]; then
  set -x
fi

if [ ! -d /repo/.git ]; then
  echo 'ERROR: Git repository must be mounted in /repo!'
  exit 1
fi

# Config
REPO_USERNAME="${REPO_USERNAME:-circleci-repo}"

sudo groupadd --gid "${REPO_GID:-1001}" "$REPO_USERNAME"
sudo useradd --uid "${REPO_UID:-1001}" --gid "$REPO_USERNAME" --shell /bin/bash --create-home "$REPO_USERNAME"
sudo sh -c "echo '${REPO_USERNAME} ALL=NOPASSWD: ALL' >> /etc/sudoers.d/100-${REPO_USERNAME}"

echo 'Defaults env_keep += "DEBUG TESTS CI FORCE_COLOR PROXY_URL"' >> "/etc/sudoers.d/200-env"

sudo -u "$REPO_USERNAME" -- "$@"
