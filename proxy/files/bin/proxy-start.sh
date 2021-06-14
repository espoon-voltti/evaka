#!/bin/sh -eu

# SPDX-FileCopyrightText: 2017-2020 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

# For log tagging (with a default value and error logging without crashing)
# shellcheck disable=SC2155
export HOST_IP=$(curl --silent --fail --show-error http://169.254.169.254/latest/meta-data/local-ipv4 || printf 'UNAVAILABLE')

if [ "${STATIC_FILES_ENDPOINT_URL:-X}" = 'X' ]; then
  echo 'ERROR: STATIC_FILES_ENDPOINT_URL must be a non-empty string!'
  exit 1
fi
if [ "${ENDUSER_GW_URL:-X}" = 'X' ]; then
  echo 'ERROR: ENDUSER_GW_URL must be a non-empty string!'
  exit 1
fi
if [ "${INTERNAL_GW_URL:-X}" = 'X' ]; then
  echo 'ERROR: INTERNAL_GW_URL must be a non-empty string!'
  exit 1
fi

if [ "${DEPLOYMENT_BUCKET:-X}" != 'X' ]; then
  s3download "$DEPLOYMENT_BUCKET" "proxy" /etc/nginx/
fi

for template in /etc/nginx/conf.d/*.template; do
    if ! test -f "$template"; then
      continue
    fi
    target=$(echo "$template" | sed -e "s/.template$//")

    erb "$template" > "$target"
done

if [ "${BASIC_AUTH_ENABLED:-false}" = 'true' ]; then
  echo "$BASIC_AUTH_CREDENTIALS" > /etc/nginx/.htpasswd
fi

exec nginx -g "daemon off;"
