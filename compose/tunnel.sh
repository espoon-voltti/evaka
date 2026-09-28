#!/usr/bin/env bash

# SPDX-FileCopyrightText: 2017-2026 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

# Exposes the local development environment through a Cloudflare quick tunnel
# so that it can be used from external devices. See compose/TUNNEL.md.
#
# Usage: mise tunnel [start|stop|status]

set -euo pipefail

cd "$(dirname "$0")"

STATE_DIR=.tunnel
LOG_FILE="$STATE_DIR/cloudflared.log"
MISE_LOCAL_TOML=../mise.local.toml
FRONTEND_PORT="${EVAKA_FRONTEND_PORT:-9099}"

tunnel_pid() {
  pgrep -f "cloudflared tunnel .*--url http://localhost:$FRONTEND_PORT\$" | head -n 1 || true
}

tunnel_url_from_log() {
  grep -oE 'https://[a-z0-9-]+\.trycloudflare\.com' "$LOG_FILE" 2>/dev/null \
    | grep -v '^https://api\.' \
    | head -n 1 || true
}

# dummy-idp and the pm2 apps read TUNNEL_URL only when they start
restart_affected_services() {
  if [ -n "$(docker compose ps -q dummy-idp 2>/dev/null)" ]; then
    echo "Recreating dummy-idp..."
    docker compose up -d dummy-idp
  fi

  local running_apps=()
  for app in apigw frontend service; do
    if [ -n "$(pm2 pid "$app" 2>/dev/null)" ]; then
      running_apps+=("$app")
    fi
  done
  if [ ${#running_apps[@]} -gt 0 ]; then
    echo "Restarting ${running_apps[*]}..."
    pm2 delete "${running_apps[@]}" > /dev/null
    pm2 start --only "$(IFS=,; echo "${running_apps[*]}")" > /dev/null
  else
    echo "The development environment is not running. Start it with: mise start"
  fi
}

start() {
  if ! command -v cloudflared > /dev/null; then
    echo "cloudflared is not installed. Install it with e.g.: brew install cloudflared"
    exit 1
  fi

  if [ -n "$(tunnel_pid)" ]; then
    echo "Tunnel is already running: ${TUNNEL_URL:-$(tunnel_url_from_log)}"
    echo "Run 'mise tunnel stop' first if you want a new one."
    exit 0
  fi

  mkdir -p "$STATE_DIR"
  echo "Starting a Cloudflare quick tunnel to http://localhost:$FRONTEND_PORT..."
  nohup cloudflared tunnel --no-autoupdate --url "http://localhost:$FRONTEND_PORT" > "$LOG_FILE" 2>&1 &

  local url=""
  for _ in $(seq 60); do
    if [ -z "$(tunnel_pid)" ]; then
      echo "cloudflared exited unexpectedly:"
      tail -n 20 "$LOG_FILE"
      if grep -q 'no such host' "$LOG_FILE"; then
        echo ""
        echo "Your DNS resolver cannot resolve *.trycloudflare.com."
        echo "Compare 'dig api.trycloudflare.com' with 'dig api.trycloudflare.com @1.1.1.1'."
      fi
      exit 1
    fi
    url=$(tunnel_url_from_log)
    if [ -n "$url" ] && grep -q 'Registered tunnel connection' "$LOG_FILE"; then
      break
    fi
    sleep 1
  done

  if [ -z "$url" ]; then
    echo "Timed out waiting for the tunnel. See $PWD/$LOG_FILE"
    stop_cloudflared
    exit 1
  fi

  mise set --file "$MISE_LOCAL_TOML" "TUNNEL_URL=$url"
  export TUNNEL_URL="$url"
  restart_affected_services

  echo ""
  echo "Tunnel is up: $url"
  echo "It may take a minute until the hostname resolves everywhere."
}

stop_cloudflared() {
  local pid
  pid=$(tunnel_pid)
  if [ -n "$pid" ]; then
    kill "$pid"
    echo "Stopped cloudflared (pid $pid)."
  fi
}

stop() {
  stop_cloudflared
  if [ -n "${TUNNEL_URL:-}" ]; then
    mise unset --file "$MISE_LOCAL_TOML" TUNNEL_URL
    unset TUNNEL_URL
    restart_affected_services
  fi
  echo "The development environment is served at http://localhost:$FRONTEND_PORT again."
}

status() {
  local pid
  pid=$(tunnel_pid)
  if [ -n "$pid" ]; then
    echo "cloudflared:  running (pid $pid)"
  else
    echo "cloudflared:  not running"
  fi
  echo "TUNNEL_URL:   ${TUNNEL_URL:-(not set)}"
  check
}

check() {
  if [ -n "${TUNNEL_URL:-}" ] && [ -z "$(tunnel_pid)" ]; then
    echo ""
    echo "Warning: TUNNEL_URL is set, but the tunnel is not running."
    echo "Run 'mise tunnel start' for a new tunnel or 'mise tunnel stop' to go back to localhost."
  fi
}

case "${1:-start}" in
  start) start ;;
  stop) stop ;;
  status) status ;;
  check) check ;;
  *)
    echo "Usage: $0 [start|stop|status]"
    exit 1
    ;;
esac
