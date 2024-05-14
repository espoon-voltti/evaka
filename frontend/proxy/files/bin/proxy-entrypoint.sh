#!/bin/sh -eu

# SPDX-FileCopyrightText: 2017-2021 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

# For log tagging (with a default value and error logging without crashing)
# shellcheck disable=SC2155

set -eu

if [ -z "${NGINX_ENTRYPOINT_QUIET_LOGS:-}" ]; then
    exec 3>&1
else
    exec 3>/dev/null
fi

if [ "${DEBUG:-false}" = "true" ]; then
  set -x
fi

export HOST_IP="UNAVAILABLE"

if [ "${ENDUSER_GW_URL:-X}" = 'X' ]; then
  echo 'ERROR: ENDUSER_GW_URL must be a non-empty string!'
  exit 1
fi
if [ "${INTERNAL_GW_URL:-X}" = 'X' ]; then
  echo 'ERROR: INTERNAL_GW_URL must be a non-empty string!'
  exit 1
fi

if test -z "${DD_PROFILING_ENABLED:-}"; then
  export DD_PROFILING_ENABLED="false"
fi

if [ "${DD_PROFILING_ENABLED}" = "true" ]; then
  if test -z "${DD_AGENT_HOST:-}"; then
    echo "ERROR: DD_AGENT_HOST missing"
    exit 1
  fi
  if test -z "${DD_AGENT_PORT:-}"; then
    echo "ERROR: DD_AGENT_PORT missing"
    exit 1
  fi
else
  export DD_AGENT_HOST="localhost"
  export DD_AGENT_PORT="8126"
fi

if [ "${DEPLOYMENT_BUCKET:-X}" != 'X' ]; then
  s3download "$DEPLOYMENT_BUCKET" "proxy" /etc/nginx/
fi

for template in /etc/nginx/conf.d/*.template /etc/nginx/*.template /internal/*.template; do
    if ! test -f "$template"; then
      continue
    fi
    target=$(echo "$template" | sed -e "s/.template$//")

    erb -T - "$template" > "$target"
done

mkdir -p /static/.well-known
cp /internal/security.txt /static/.well-known/

if [ "${DEBUG:-false}" = "true" ]; then
  cat /etc/nginx/nginx.conf
fi

if [ "${BASIC_AUTH_ENABLED:-false}" = 'true' ]; then
  echo "$BASIC_AUTH_CREDENTIALS" > /etc/nginx/.htpasswd
fi

exec "$@"
