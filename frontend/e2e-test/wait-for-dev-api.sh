#!/bin/sh

# SPDX-FileCopyrightText: 2017-2020 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

TRIES=1
STATUS=$(curl -w "%{http_code}" --connect-timeout 3 -X POST "$1/api/internal/dev-api/clean-up")

while [ "$STATUS" != "204" ]; do
  if [ $TRIES -lt 37 ]; then
    sleep 5s
    TRIES=$((TRIES+1))
    STATUS=$(curl -w "%{http_code}" --connect-timeout 3 -X POST "$1/api/internal/dev-api/clean-up")
  else
    echo "dev api did not answer in $TRIES tries"
    exit 1
  fi
done
