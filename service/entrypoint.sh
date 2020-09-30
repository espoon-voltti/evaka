#!/usr/bin/env bash

# SPDX-FileCopyrightText: 2017-2020 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

set -euo pipefail

# For logback tagging (allow for local fallback)
export HOST_IP=$(wget -qO- http://169.254.169.254/latest/meta-data/local-ipv4 || printf 'UNAVAILABLE')

# Download deployment specific files from S3 if in a non-local environment
if [ "${VOLTTI_ENV:-X}" != "local" ]; then
  s3download "$DEPLOYMENT_BUCKET" evaka-srv /home/evaka/s3
fi

# Run as exec so the application can receive any Unix signals sent to the container, e.g.,
# Ctrl + C.
exec java -cp . -server $JAVA_OPTS org.springframework.boot.loader.JarLauncher "$@"
