#!/bin/bash

# SPDX-FileCopyrightText: 2017-2021 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

set -eu

cd "$( dirname "${BASH_SOURCE[0]}")"

TEST_IMAGE="evaka/test:local"
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

CHECK_COMMAND="erb -T- /ci/template.yml | tee /dev/tty | circleci --skip-update-check config validate -"

if test -z "${1:-}" || [ "${1:-}" = "fail" ]; then
    if docker run -v "$(pwd):/ci" \
           -it "$TEST_IMAGE" erb -T- /ci/template.yml; then
        echo "Unexpected success"
        exit 1
    fi
    echo "Failure is expected"
fi

if test -z "${1:-}" || [ "${1:-}" = "master" ]; then
    docker run -v "$(pwd):/ci" \
        -e CIRCLE_BRANCH=master \
        -it "$TEST_IMAGE" bash -c "$CHECK_COMMAND"
fi

if test -z "${1:-}" || [ "${1:-}" = "pull" ]; then
    docker run -v "$(pwd):/ci" \
        -e CIRCLE_BRANCH=pull/1234 \
        -it "$TEST_IMAGE" bash -c "$CHECK_COMMAND"
fi
