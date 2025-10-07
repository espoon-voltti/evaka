#!/bin/bash

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

for directory in /etc/nginx/conf.d/ /etc/nginx/ /internal/; do
  gomplate --input-dir="$directory" --output-map="$directory"'{{ .in | strings.ReplaceAll ".template" "" }}'
done

mkdir -p /static/.well-known
cp /internal/security.txt /static/.well-known/

if [ "${DEBUG:-false}" = "true" ]; then
  cat /etc/nginx/nginx.conf
fi

if [ "${BASIC_AUTH_ENABLED:-false}" = "true" ]; then
  echo "$BASIC_AUTH_CREDENTIALS" > /etc/nginx/.htpasswd
fi

if test -n "${WAIT_UPSTREAM_URLS:-}"; then
  IFS=';' read -ra URLS <<< "$WAIT_UPSTREAM_URLS"
  for URL in "${URLS[@]}"; do
    HOST_PORT_PATH="${URL#*://}"      # host:port/path
    HOST_PORT="${HOST_PORT_PATH%%/*}" # host:port
    HOST="${HOST_PORT%%:*}"
    PORT="${HOST_PORT##*:}"

    ready="false"
    for _try in $(seq "${MAX_RETRIES:-60}"); do
      echo "Checking ${URL} ..."

      output="$(bash -c "exec 3<>/dev/tcp/${HOST}/${PORT}; echo -e \"GET ${URL} HTTP/1.0\r\n\r\n\" >&3; timeout 5 cat <&3" 2> /dev/null || true)"

      if echo "$output" | grep -q 'HTTP/1.1 200' ; then
          echo "$URL is ready!"
          ready="true"
          break
      else
        echo -n "URL ${URL} not ready: "
        echo "$output" | grep 'HTTP' || echo "Connection failed"
      fi

      sleep "${RETRY_DELAY:-5}"
    done

    if [ "$ready" = "false" ]; then
      echo "ERROR: Waiting upstream ${URL} failed."
      exit 1
    fi
  done
fi

exec "$@"
