#!/bin/bash

# SPDX-FileCopyrightText: 2017-2021 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

# Usage (run in repoitory root):
#
# docker run --rm -it \
#   --volume "${PWD}/bin/circleci-e2e-run.sh":/tmp/circleci-e2e-run.sh:ro \
#   --volume "${PWD}":/repo:rw \
#   --network=compose_default \
#   --env REPO_UID="$UID" \
#   --env CI="$CI" \
#   --entrypoint=/bin/bash \
#   cimg/node:14.15-browsers \
#   /tmp/circleci-e2e-run.sh <testcafe|playwright>

set -euo pipefail

if [ "${DEBUG:-X}" = "true" ]; then
  set -x
fi

if [ "${1:-X}" = "X" ]; then
  echo "Usage: $0 <testcafe|playwright> [test-suite]"
  exit 1
fi

if [ ! -d /repo/.git ]; then
  echo 'ERROR: Git repository must be mounted in /repo!'
  exit 1
fi

# Config
REPO_UID=${REPO_UID:-1001}
REPO_GID=${REPO_UID:-1001}
REPO_USERNAME=${REPO_USERNAME:-circleci-repo}
TEST_RUNNER=$1

# Create user matching mounted repo in order to interact with it
{
  sudo groupadd --gid "$REPO_GID" "$REPO_USERNAME" \
  && sudo useradd --uid "$REPO_UID" --gid "$REPO_USERNAME" --shell /bin/bash --create-home "$REPO_USERNAME" \
  && sudo sh -c "echo '${REPO_USERNAME} ALL=NOPASSWD: ALL' >> /etc/sudoers.d/100-${REPO_USERNAME}"
} || true

if [ "$TEST_RUNNER" = "testcafe" ]; then
  # Install Testcafe dependencies
  sudo apt-get update && sudo apt-get --yes install --no-install-recommends ffmpeg

  # Install Google Chrome
  curl --silent --show-error --location --fail --retry 3 --output /tmp/google-chrome-stable_current_amd64.deb https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb
  sudo dpkg -i /tmp/google-chrome-stable_current_amd64.deb
  rm -rf /tmp/google-chrome-stable_current_amd64.deb
  sudo sed -i 's|HERE/chrome"|HERE/chrome" --disable-setuid-sandbox --no-sandbox|g' "/opt/google/chrome/google-chrome"
  google-chrome --version

  # Install matching Chromedriver
  CHROME_VERSION="$(google-chrome --version)"
  CHROMEDRIVER_RELEASE=${CHROME_VERSION//Google Chrome /}
  export CHROMEDRIVER_RELEASE
  export CHROMEDRIVER_RELEASE=${CHROMEDRIVER_RELEASE%%.*}
  CHROMEDRIVER_VERSION=$(curl --silent --show-error --location --fail --retry 4 --retry-delay 5 "http://chromedriver.storage.googleapis.com/LATEST_RELEASE_${CHROMEDRIVER_RELEASE}")
  curl --silent --show-error --location --fail --retry 4 --retry-delay 5 --output /tmp/chromedriver_linux64.zip "http://chromedriver.storage.googleapis.com/${CHROMEDRIVER_VERSION}/chromedriver_linux64.zip"
  cd /tmp
  unzip chromedriver_linux64.zip
  rm -f chromedriver_linux64.zip
  sudo mv chromedriver /usr/local/bin/chromedriver
  sudo chmod +x /usr/local/bin/chromedriver
  chromedriver --version
fi

# In order to allow yarn and test runners to write to mounted repository,
# execute yarn install and tests as a user with the same UID.
sudo su - "$REPO_USERNAME" <<EOF
  set -euo pipefail

  # Preserve these variables from the initial environment
  export CI="$CI"

  if [ "${DEBUG:-X}" = "true" ]; then
    set -x
  fi

  cd /repo/frontend
  yarn install --immutable --immutable-cache

  # TODO: Is there any better way than this?
  if [ "$TEST_RUNNER" = "playwright" ]; then
    # Make Playwright download browsers to node_modules/playwright instead of $HOME/.cache/ms-playwright
    export PLAYWRIGHT_BROWSERS_PATH=0
    # and force yarn to re-download playwright to ensure its postinstall script
    # is actually triggered and the browsers downloaded.
    yarn rebuild playwright
  fi

  echo 'INFO: Waiting for compose stack to be up...'
  ./wait-for-dev-api.sh 'http://evaka-proxy:8080'

  if [ "$TEST_RUNNER" = "playwright" ]; then
    FILENAMES=(\$(cat playwright-filenames.txt))
    yarn e2e-ci-playwright "\${FILENAMES[@]}"
  elif [ "$TEST_RUNNER" = "testcafe" ]; then
    yarn e2e-ci-testcafe --fixture-grep "\$(cat testcafe-fixture-regex.txt)" -- src/e2e-test/specs/
  else
    echo 'ERROR: Invalid test runner: $TEST_RUNNER'
  fi
EOF
