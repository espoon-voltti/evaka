#!/usr/bin/env bash

# SPDX-FileCopyrightText: 2017-2020 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

set -euo pipefail

# For logs
if [ "${EC2_HOST:-false}" = "true" ]; then
  HOST_IP="$(curl --silent --fail --show-error http://169.254.169.254/latest/meta-data/local-ipv4 || printf 'UNAVAILABLE')"
  export HOST_IP
elif test -n "${ECS_CONTAINER_METADATA_URI:-}"; then
  JSON="$(curl --silent --fail --show-error "${ECS_CONTAINER_METADATA_URI}"/task || printf 'UNAVAILABLE')"
  HOST_IP="$(echo "$JSON" | jq -r '.Containers[0].Networks[0].IPv4Addresses[0]' || printf 'UNAVAILABLE')"
  export HOST_IP
else
  export HOST_IP="UNAVAILABLE"
fi

# Download deployment specific files from S3 if in a non-local environment
if [ "${VOLTTI_ENV:-X}" != "local" ]; then
  s3download "$DEPLOYMENT_BUCKET" "api-gw" /home/evaka/s3
fi

# This fixes the issue with Docker not shutting down correctly when using pipes and subshells
# Adapted from https://mattias.holmlund.se/2019/01/logging-with-pino-from-docker/
pid=0

# SIGTERM-handler
function term_handler() {
  if [ $pid -ne 0 ]; then
    kill -SIGTERM "$pid"
    wait "$pid"
  fi
  exit 143; # 128 + 15 -- SIGTERM
}

# On SIGTERM, execute term_handler
trap 'term_handler' SIGTERM

# TL;DR: The redirection trick makes sure that $! is the pid of the main Node.js process.
# Redirects the output of the main Node.js child process to the pino-cli child process using process substitution.
# This changes the order in which background processes are started, so that $! gives the PID of the main Node.js
# process. See https://stackoverflow.com/a/8048493.
# If pino-cli exits with non-zero status code, we kill the parent process by sending a SIGTERM signal to it. Note
# that the exit status for the Docker container will be 143 in this case although it should be the exit status of
# pino-cli, e.g., 1. Storing the exit status of the pino-cli child process would require using bash named
# pipes which would make the implementation a bit more complex. For simplicity, the current solution is good enough.
"$@" > >(node ./dist/pino-cli/cli/bin.js || kill $$) &
pid="$!"

# Keep the Docker container from exiting by waiting for a status change in the main Node.js child process
wait $pid
