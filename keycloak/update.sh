#!/bin/bash

# SPDX-FileCopyrightText: 2017-2023 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

set -euo pipefail

cd "$( dirname "${BASH_SOURCE[0]}" )"

if test -z "${1:-}" || test -z "${2:-}"; then
    echo "Fetch sources and generate diff files"
    echo "Used to find out differences in source files"
    echo ""
    echo "Usage: $0 <old-version> <new-version>"
    exit 1
fi

TARGET_PATH="update"

mkdir -p "$TARGET_PATH"
cd "$TARGET_PATH"

REVISIONS=("$1" "$2")

BASE_URL="https://raw.githubusercontent.com/keycloak/keycloak"

FILE_PATH="services/src/main/java/org/keycloak/authentication/authenticators/broker"
FILE_NAMES=("IdpReviewProfileAuthenticatorFactory.java" "IdpReviewProfileAuthenticator.java")

for filename in "${FILE_NAMES[@]}"; do
    for revision in "${REVISIONS[@]}"; do
        curl -sSf "${BASE_URL}/${revision}/${FILE_PATH}/${filename}" -o "${filename}.${revision}.orig"
        echo "Downloaded: $TARGET_PATH/${filename}.${revision}.orig"
    done
    diff -u "${filename}.${REVISIONS[0]}.orig" "${filename}.${REVISIONS[1]}.orig" > "${filename}.diff.new" || true
    echo "Generated $TARGET_PATH/${filename}.diff.new"
done

filename="pom.xml"
for revision in "${REVISIONS[@]}"; do
    curl -sSf "${BASE_URL}/${revision}/${filename}" -o "${filename}.${revision}.orig"
    echo "Downloaded: $TARGET_PATH/${filename}.${revision}.orig"
done
diff -u "${filename}.${REVISIONS[0]}.orig" "${filename}.${REVISIONS[1]}.orig" > "${filename}.diff.new" || true
echo "Generated $TARGET_PATH/${filename}.diff.new"
