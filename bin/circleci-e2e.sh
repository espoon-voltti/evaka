#!/bin/bash

# SPDX-FileCopyrightText: 2017-2021 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

# Run E2E tests inside a container in CircleCI with the app running in compose.
#
# Usage:
#   1. Start compose stack
#   2. Create split test split file under frontend/ for target test runner
#   3. Run this script

set -euo pipefail

if [ "${DEBUG:-X}" = "true" ]; then
  set -x
fi

if [ "${1:-X}" = "X" ]; then
  echo "Usage: $0 playwright"
  exit 1
fi

COMPOSE_NETWORK=${COMPOSE_NETWORK:-compose_default}
SKIP_SPLIT=${SKIP_SPLIT:-false}
PLAYWRIGHT_VERSION=${PLAYWRIGHT_VERSION:-v1.18.1}
TEST_RUNNER=$1

# Ensure we are in repository root
pushd "$(dirname "${BASH_SOURCE[0]}")"/..

if [ "$SKIP_SPLIT" != 'true' ]; then
  pushd frontend
  # Get list of test files that should run on this node.
  if [ "$TEST_RUNNER" = "playwright" ]; then
    mapfile -t FILENAMES < <(circleci tests glob \
        'src/e2e-test/specs/**/*.spec.ts' \
        | sort -h \
        | circleci tests split --split-by=timings --timings-type=filename)
    printf '%s\n' 'Spec files selected for node:' "${FILENAMES[@]}"
    printf "%s\n" "${FILENAMES[@]}" > playwright-filenames.txt
  else
    echo "ERROR: Invalid test_runner: $TEST_RUNNER"
    exit 1
  fi
  popd
fi

pushd compose

./test-e2e run "$TEST_RUNNER"
