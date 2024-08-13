#!/usr/bin/env bash

# SPDX-FileCopyrightText: 2017-2020 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

set -euo pipefail

# For log tagging (with a default value and error logging without crashing)
# shellcheck disable=SC2155
export HOST_IP=$(curl --silent --fail --show-error http://169.254.169.254/latest/meta-data/local-ipv4 || printf 'UNAVAILABLE')

# Download deployment specific files from S3 if in a non-local environment
if [ "${VOLTTI_ENV:-X}" != "local" ]; then
  s3download "$DEPLOYMENT_BUCKET" evaka-srv /home/evaka/s3
fi

# Run as exec so the application can receive any Unix signals sent to the container, e.g.,
# Ctrl + C.
if [ "${DD_PROFILING_ENABLED:-false}" = "true" ]; then
  export DD_AGENT_HOST="${DD_AGENT_HOST:-localhost}"
  export DD_TRACE_AGENT_PORT="${DD_TRACE_AGENT_PORT:-8126}"
  export DD_JMXFETCH_STATSD_HOST="${DD_JMXFETCH_STATSD_HOST:-$HOST_IP}"
  export DD_ENV="${DD_ENV:-$VOLTTI_ENV}"
  export DD_VERSION="${DD_VERSION:-$APP_COMMIT}"
  export DD_SERVICE="${DD_SERVICE:-$APP_NAME}"
  export DD_TRACE_OTEL_ENABLED=true

  if [ "$DD_AGENT_HOST" = "UNAVAILABLE" ]; then
    echo "Invalid DD_AGENT_HOST. Is it unset and not in AWS environment?"
    exit 1
  fi

  # shellcheck disable=SC2086
  exec java \
    -Ddd.jmxfetch.config=/etc/jmxfetch/conf.yaml \
    -Ddd.profiling.enabled=true \
    -Ddd.logs.injection=true \
    -Ddd.trace.sample.rate=1 \
    -javaagent:/opt/dd-java-agent.jar \
    -XX:FlightRecorderOptions=stackdepth=256 \
    -cp . -server $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher "$@"
else
  # shellcheck disable=SC2086
  exec java -cp . -server $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher "$@"
fi
