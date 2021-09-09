#!/bin/sh

set -e

SRC_DIR=src
DIST_DIR=dist
ASSET_EXTENSIONS="css woff woff2 otf ttf eot svg png gif jpg ico"

compile_ts() {
  yarn tsc --build "$@"
}

watch_ts() {
  compile_ts --watch --preserveWatchOutput --pretty
}

asset_pattern() {
  echo "( -name *.$(echo "$1" | sed "s/ / -o -name *./g") )"
}

copy_assets() {
  # shellcheck disable=SC2046
  find "$SRC_DIR" $(asset_pattern "$ASSET_EXTENSIONS") -exec sh -c '
    to_root=$0
    source_file=$1
    dest_dir=$to_root/$(dirname "${source_file#*/}")
    mkdir -p $dest_dir
    cp -p $source_file $dest_dir' "$DIST_DIR" {} \;
}

bundle() {
  yarn webpack --mode "$1"
}

is_compilation_done() {
  echo "$1" | grep -Eq "Found \d+ errors"
}

dev_server_pid=
maybe_start_dev_server() {
  [ -n "$dev_server_pid" ] && return
  echo "Starting dev-server"
  node dev-server.js &
  dev_server_pid=$!
}

maybe_kill_dev_server() {
  [ -z "$dev_server_pid" ] && return
  kill "$dev_server_pid"
}

dev() {
  trap maybe_kill_dev_server INT TERM EXIT

  echo "Starting TypeScript compiler in watch mode"
  watch_ts | while read -r line; do
    echo "$line"
    if is_compilation_done "$line"; then
      copy_assets
      maybe_start_dev_server
    fi
  done
}

build() {
  echo "Compiling TypeScript..."
  compile_ts
  echo "Copying assets..."
  copy_assets
  echo "Bundling for $1..."
  bundle "$1"
}

case "$1" in
  build-dev) build development ;;
  build-prod) build production ;;
  dev) dev ;;
  *) echo "Unrecognized command: $1" && exit 1 ;;
esac
