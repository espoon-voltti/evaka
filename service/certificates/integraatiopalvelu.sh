#!/bin/bash

# SPDX-FileCopyrightText: 2017-2023 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

set -euo pipefail

if [ "${DEBUG:-"false"}" = "true" ]; then
    set -x
fi

CERTIFICATE_FILES=(
    "ServerCertificate.crt"
    "Root.crt"
    "Intermediate.crt"
)

# Reuse tool likes pem files
CERTIFICATE_PEM_FILES=(
    "ServerCertificate.pem"
    "Root.pem"
    "Intermediate.pem"
)

CACHE_PATH="/tmp/create-integraatiopalvelu"

certificate_download() {
    name="$1"
    url_base="${2:-"https://valtori.fi/documents/7128404/8370217/"}"
    suffix="${3:-".zip"}"

    target_zip="${CACHE_PATH}/${name}.zip"

    if ! test -f "$target_zip"; then
        curl --silent --fail --show-error \
            "${url_base}/${name}${suffix}" \
            -o "$target_zip"
    fi

    if test -d "${name}"; then
        rm -r "${name}"
    fi
    mkdir -p "${name}"
    cd "${name}"
    unzip "$target_zip"
    for index in "${!CERTIFICATE_FILES[@]}"; do
        mv "${CERTIFICATE_FILES[$index]}" "${CERTIFICATE_PEM_FILES[$index]}"
    done
    cd - > /dev/null
}

certificate_add() {
    name="$1"
    target_file="$2"

    for file in "${CERTIFICATE_PEM_FILES[@]}"; do
        keytool -delete -keystore "${target_file}" -alias "${name}-${file}" \
            -storepass:env "TRUSTSTORE_PASSWORD" -noprompt > /dev/null || true
        keytool -importcert -alias "${name}-${file}" -file "${name}/${file}" \
            -keystore "${target_file}" -storepass:env "TRUSTSTORE_PASSWORD" -storetype PKCS12  -noprompt
    done
}

cd "$( dirname "${BASH_SOURCE[0]}")"

ACTION="${1:-usage}"

if [ "${CLEAN:-"false"}" = "true" ]; then
    rm -rf ${CACHE_PATH}
fi

mkdir -p ${CACHE_PATH}

if [ "$ACTION" = "download" ]; then
    certificate_download "qat.integraatiopalvelu.fi_entrust_2023" # Valid from 2023-03-02 to 2024-02-29
    certificate_download "pr0.integraatiopalvelu.fi_entrust_2023" # Valid from 2023-03-09 to 2024-02-29

    certificate_download "qat.integraatiopalvelu.fi_2024"
    certificate_download "pr0.integraatiopalvelu.fi_2024"
elif [ "$ACTION" = "truststore" ]; then
    if test -z "$TRUSTSTORE_PASSWORD"; then
        echo "Error: TRUSTSTORE_PASSWORD environment variable missing."
        exit 1
    fi
    if test -d "truststore"; then
        rm -r truststore
    fi
    mkdir -p truststore
    qa_filename="truststore/qa.p12"

    certificate_add "qat.integraatiopalvelu.fi_entrust_2023" "$qa_filename" # Valid from 2023-03-02 to 2024-02-29

    certificate_add "qat.integraatiopalvelu.fi_2024" "$qa_filename"

    production_filename="truststore/production.p12"

    certificate_add "pr0.integraatiopalvelu.fi_entrust_2023" "$production_filename" # Valid from 2023-03-09 to 2024-02-29

    certificate_add "pr0.integraatiopalvelu.fi_2024" "$production_filename"

else
    echo "Usage: $0 download|truststore"
    echo "  download:   download certificates"
    echo "  truststore: create truststore"
    exit 1
fi
