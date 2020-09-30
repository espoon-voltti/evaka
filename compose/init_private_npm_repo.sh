#!/usr/bin/env bash

# SPDX-FileCopyrightText: 2017-2020 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

set -euo pipefail

NPM_REPO_TOKEN=$1
FONTAWESOME_TOKEN=$2

PROJECTS=(frontend)
for PROJECT in "${PROJECTS[@]}"; do
    pushd ../"$PROJECT" > /dev/null

    if [ ! -f ".npmrc" ]; then
        echo "Setting up ${PROJECT}/.npmrc"

cat << EOF > .npmrc
@fortawesome:registry=https://npm.fontawesome.com/
//npm.fontawesome.com/:_authToken="${FONTAWESOME_TOKEN}"
@voltti:registry=https://npm.sst.espoon-voltti.fi
//npm.sst.espoon-voltti.fi/:_authToken="${NPM_REPO_TOKEN}"
//npm.sst.espoon-voltti.fi/:always-auth=true
EOF

    else
        echo "Found existing ${PROJECT}/.npmrc so not setting up a new one"
    fi

    popd > /dev/null
done
