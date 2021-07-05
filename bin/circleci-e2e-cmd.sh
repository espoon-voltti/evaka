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
REPO_UID=${REPO_UID:-1001}
REPO_GID=${REPO_UID:-1001}
REPO_USERNAME=${REPO_USERNAME:-circleci-repo}
PROXY_URL=${PROXY_URL:-http://evaka-proxy:8080}
TEST_RUNNER=$1

if [ "$TEST_RUNNER" = "testcafe" ]; then
  # Create user matching mounted repo in order to interact with it
  {
    sudo groupadd --gid "$REPO_GID" "$REPO_USERNAME" \
    && sudo useradd --uid "$REPO_UID" --gid "$REPO_USERNAME" --shell /bin/bash --create-home "$REPO_USERNAME" \
    && sudo sh -c "echo '${REPO_USERNAME} ALL=NOPASSWD: ALL' >> /etc/sudoers.d/100-${REPO_USERNAME}"
  } || true

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

  # In order to allow yarn and test runners to write to mounted repository,
  # execute yarn install and tests as a user with the same UID.
  sudo su - "$REPO_USERNAME" <<EOF
    set -euo pipefail

    # Preserve these variables from the initial environment
    export DEBUG="$DEBUG"
    export CI="$CI"
    # Misc configs
    export FORCE_COLOR=1

    if [ "${DEBUG:-X}" = "true" ]; then
      set -x
    fi

    cd /repo/frontend

    echo 'INFO: Waiting for compose stack to be up...'
    ./wait-for-dev-api.sh '$PROXY_URL'

    yarn e2e-ci-testcafe --fixture-grep "\$(cat testcafe-fixture-regex.txt)" -- src/e2e-test/specs/
EOF
elif [ "$TEST_RUNNER" = "playwright" ]; then
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
