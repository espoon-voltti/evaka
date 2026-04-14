#!/usr/bin/env bash

# SPDX-FileCopyrightText: 2017-2026 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

input=$(cat)
cwd=$(echo "$input" | jq -r '.workspace.current_dir // .cwd // empty')
model=$(echo "$input" | jq -r '.model.display_name // empty')
used=$(echo "$input" | jq -r '.context_window.used_percentage // empty')

parts=()
[ -n "$cwd" ] && parts+=("$cwd")
[ -n "$model" ] && parts+=("$model")
if [ -n "$used" ]; then
  parts+=("$(printf 'ctx: %.0f%%' "$used")")
fi

printf '%s' "$(IFS=' | '; echo "${parts[*]}")"
