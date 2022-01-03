#!/bin/bash

# SPDX-FileCopyrightText: 2017-2022 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

set -eou pipefail

if [ "${DIABLE_JSON_LOG:-false}" = "true" ]; then
    # Local environment it is easier to read normal loggig
    rm -f /opt/jboss/keycloak/standalone/configuration/logging.properties.template
    rm -f /opt/jboss/startup-scripts/logging.cli.template
fi

if test -f /opt/jboss/keycloak/standalone/configuration/logging.properties.template; then
    sed -e "s/\${VOLTTI_ENV}/${VOLTTI_ENV:-local}/" \
        /opt/jboss/keycloak/standalone/configuration/logging.properties.template \
            > /opt/jboss/keycloak/standalone/configuration/logging.properties
fi

if test -f /opt/jboss/startup-scripts/logging.cli.template; then
    sed -e "s/\${VOLTTI_ENV}/${VOLTTI_ENV:-local}/" \
        /opt/jboss/startup-scripts/logging.cli.template \
            > /opt/jboss/startup-scripts/logging.cli
    chmod +x /opt/jboss/startup-scripts/logging.cli
    rm /opt/jboss/startup-scripts/logging.cli.template
fi

/opt/jboss/tools/docker-entrypoint.sh "$@"
