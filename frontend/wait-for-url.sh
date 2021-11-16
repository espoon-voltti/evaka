#!/bin/sh

# SPDX-FileCopyrightText: 2017-2020 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

url=$1
code=${2:-204}
healthcheck() {
	curl -sSw "%{http_code}" --connect-timeout 3 --max-time 5 "$url" -o /dev/null
}

TRIES=37

while [ $TRIES -gt 0 ]; do
	STATUS=$(healthcheck)

	if [ "$STATUS" = "${code}" ]; then
		exit 0
	fi
	echo "Got $STATUS for $url - retrying ..."
	sleep 5s
	TRIES=$((TRIES - 1))
done

echo "$url not answer $code in $TRIES tries"
exit 1
