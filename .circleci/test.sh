#!/bin/bash

# SPDX-FileCopyrightText: 2017-2021 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

# Local sanity check test for template config
# Run ./test.sh to test all
# Run ./test.sh <fail|master|pull> to run single test

set -eu

cd "$( dirname "${BASH_SOURCE[0]}")"

TEST_IMAGE="evaka/ruby-test:local"
CIRCLECI_CLI_VERSION="0.1.16535"
docker build -t "$TEST_IMAGE" - <<EOF
FROM cimg/ruby:3.0.2
RUN cd /tmp \
 && curl -sSfL "https://github.com/CircleCI-Public/circleci-cli/releases/download/v${CIRCLECI_CLI_VERSION}/circleci-cli_${CIRCLECI_CLI_VERSION}_linux_amd64.tar.gz" -o circleci-cli.tar.gz \
 && tar xzf circleci-cli.tar.gz \
 && sudo mv "circleci-cli_${CIRCLECI_CLI_VERSION}_linux_amd64/circleci" /usr/local/bin \
 && sudo chown 0:0 /usr/local/bin/circleci \
 && rm -r circleci-cli.tar.gz "circleci-cli_${CIRCLECI_CLI_VERSION}_linux_amd64/"
WORKDIR /ci
EOF

docker run --rm -v "$(pwd):/ci" -it "$TEST_IMAGE" circleci --skip-update-check config validate config.yml
