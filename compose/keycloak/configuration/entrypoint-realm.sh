#!/bin/bash

set -e

# Note this will not re-configure the realm. If you want to reconfigure realm then just delete the realm and run docker-compose down and then up.

echo "Connecting to KeyCloak ... 🔌"

timeout 60 bash -c 'while [[ "$(curl -s -o /dev/null -w ''%{http_code}'' keycloak:8080)" != "200" ]]; do sleep 5; done' || false

echo "... KeyCloak connection established 💪"

export PATH=$PATH:$JBOSS_HOME/bin
KEYCLOAK_HOST="keycloak"
KEYCLOAK_PORT="8080"

kcadm.sh config credentials --server "http://${KEYCLOAK_HOST}:${KEYCLOAK_PORT}/auth" --realm master --user "$KEYCLOAK_USER" --password "$KEYCLOAK_PASSWORD"

admin_id="$(kcadm.sh get users -r master -q username=admin --fields id --format csv --noquotes)"
kcadm.sh update "users/$admin_id" -r master -s 'email=admin@example.com' -s 'firstName=He' -s 'lastName=Man'

if [ "$(kcadm.sh get realms | grep -c '"realm" : "Evaka"')" != "1" ]; then # not best way to check exitence os realm, but we do not want to install extra tools into containers
  kcadm.sh create realms -f /configuration/evaka.json
fi

echo "KeyCloak configured!"

exit 0
