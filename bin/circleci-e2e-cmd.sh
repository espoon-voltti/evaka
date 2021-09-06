#!/bin/bash

# SPDX-FileCopyrightText: 2017-2021 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

# Usage (run in repoitory root):
#
# ./bin/circleci-e2e.sh
#
# OR:
#
# docker run --rm -it \
#   --volume "${PWD}/bin/circleci-e2e-cmd.sh":/tmp/cmd.sh:ro \
#   --volume "${PWD}":/repo:rw \
#   --network=compose_default \
#   --env REPO_UID="$UID" \
#   --env CI="$CI" \
#   --entrypoint=/bin/bash \
#   cimg/node:14.15-browsers \
#   /tmp/cmd.sh <testcafe|playwright>

set -euo pipefail

if [ "${DEBUG:-X}" = "true" ]; then
  set -x
fi

if [ "${1:-X}" = "X" ]; then
  echo "Usage: $0 <testcafe|playwright>"
  exit 1
fi

if [ ! -d /repo/.git ]; then
  echo 'ERROR: Git repository must be mounted in /repo!'
  exit 1
fi

# Config

PROXY_URL=${PROXY_URL:-http://evaka-proxy:8080}
TEST_RUNNER=$1

if [ "$TEST_RUNNER" = "playwright" ]; then
  # Misc configs
  export FORCE_COLOR=1

  cd /repo/frontend

  echo 'INFO: Waiting for compose stack to be up...'
  ./wait-for-dev-api.sh "$PROXY_URL"

  mapfile -t FILENAMES < playwright-filenames.txt
  yarn e2e-ci-playwright "${FILENAMES[@]}"
else
  echo "ERROR: Invalid test_runner: $TEST_RUNNER"
  exit 1
fi
