#!/bin/sh

# SPDX-FileCopyrightText: 2017-2020 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

# Fetch Espoo specific build customizations from a private AWS S3 bucket.
# These should mainly be unlicensed files such as logos that cannot be provided
# in this repository publically to remain REUSE compliant.
#
# This script should only be run in CI and all customization files should
# have equivalent placeholders (or application should work without them) in
# the repository.

set -eu

if [ "${DEBUG:-X}" = 'true' ]; then
  set -x
fi

if [ "${1:-X}" = '--help' ]; then
  echo 'Usage: ./bin/fetch-espoo-build-customizations [OPTIONS]'
  echo ''
  echo 'Fetch Espoo specific build customizations from a private AWS S3 bucket.'
  echo 'Replaces placeholder files during CI builds.'
  echo ''
  echo 'Options:'
  echo '    --help          Print this help'
  exit 0
fi

BUCKET_NAME=${BUCKET_NAME:-evaka-deployment-local}
# NOTE: Matching slashes in source and target
SOURCE_PATH=${SOURCE_PATH:-build-customizations/}
TARGET_PATH=${TARGET_PATH:-./espoo-customizations/}

echo "Downloading Espoo build-customizations from: s3://${BUCKET_NAME}/${SOURCE_PATH}"

aws --profile voltti-local s3 sync \
  "s3://${BUCKET_NAME}/${SOURCE_PATH}" \
  "$TARGET_PATH"

# Assert that at least something was downloaded,
# otherwise this script is unnecessary and should be removed.
if [ -z "$(git status --porcelain=v1 2>/dev/null)" ]; then
  echo 'ERROR: No files changed with build customizations!'
  echo 'The current contents of the source bucket are:'
  aws --profile voltti-local s3 ls \
    "s3://${BUCKET_NAME}/${SOURCE_PATH}" \
    --recursive
  exit 1
fi

echo "Done"
