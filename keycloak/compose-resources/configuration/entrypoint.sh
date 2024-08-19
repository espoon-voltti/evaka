#!/bin/bash

# SPDX-FileCopyrightText: 2017-2022 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

set -eou pipefail

if [ "${HTTPS_GENERATE:-false}" = "true" ]; then
    cd /opt/keycloak
    keytool -genkeypair -storepass "$HTTPS_STOREPASS" -storetype PKCS12 -keyalg RSA -keysize 2048 \
        -dname "CN=${HTTPS_DNAME}" -alias server -ext "SAN:c=${HTTPS_SAN:-"DNS:localhost,IP:127.0.0.1"}" -keystore conf/server.keystore
    cd - 2> /dev/null
fi

if test -f /configuration/evaka.json; then
    /opt/keycloak/bin/kc.sh import --file=/configuration/evaka.json
fi

exec /opt/keycloak/bin/kc.sh "$@"
