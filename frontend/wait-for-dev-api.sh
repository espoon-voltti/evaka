#!/bin/sh

# SPDX-FileCopyrightText: 2017-2020 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

url=$1

healthcheck() {
	curl -w "%{http_code}" --connect-timeout 3 "$url/api/internal/dev-api"
}

TRIES=1
STATUS=$(healthcheck)

while [ "$STATUS" != "204" ]; do
	if [ $TRIES -lt 37 ]; then
		sleep 5s
		TRIES=$((TRIES + 1))
		STATUS=$(healthcheck)
	else
		echo "dev api did not answer in $TRIES tries"
		exit 1
	fi
done
