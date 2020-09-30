#!/bin/sh -eu

# SPDX-FileCopyrightText: 2017-2020 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

# For log tagging (allow for local fallback)
HOST_IP=$(wget -qO- http://169.254.169.254/latest/meta-data/local-ipv4 || printf 'UNAVAILABLE')
export HOST_IP

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

erb /etc/nginx/conf.d/evaka-nginx.conf.template > /etc/nginx/conf.d/evaka-nginx.conf

if [ "${BASIC_AUTH_ENABLED:-false}" = 'true' ]; then
  echo "$BASIC_AUTH_CREDENTIALS" > /etc/nginx/.htpasswd
fi

exec nginx -g "daemon off;"
