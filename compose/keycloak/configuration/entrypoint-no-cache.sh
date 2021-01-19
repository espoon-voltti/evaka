#!/bin/bash

# SPDX-FileCopyrightText: 2017-2020 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

set -e

sed -i 's|<cacheThemes>.*<\/cacheThemes>|<cacheThemes>false<\/cacheThemes>|g' /opt/jboss/keycloak/standalone/configuration/standalone.xml
sed -i 's|<staticMaxAge>.*<\/staticMaxAge>|<staticMaxAge>-1<\/staticMaxAge>|g' /opt/jboss/keycloak/standalone/configuration/standalone.xml
sed -i 's|<cacheTemplates>.*<\/cacheTemplates>|<cacheTemplates>false<\/cacheTemplates>|g' /opt/jboss/keycloak/standalone/configuration/standalone.xml

exec "/opt/jboss/tools/docker-entrypoint.sh" $@
