#!/bin/bash

# SPDX-FileCopyrightText: 2017-2021 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

set -euo pipefail

ENVIRONMENT="${ENVIRONMENT:-${1:-}}"
MASTER_DAYS="${MASTER_DAYS:-30}"
BRANCH_DAYS="${BRANCH_DAYS:-7}"

FRONTENDS=(citizen employee employee/mobile maintenance-page master)

bucket="evaka-static-$ENVIRONMENT"

if [ -z "$ENVIRONMENT" ] || [ "$ENVIRONMENT" = "-h" ] || [ "$ENVIRONMENT" = "--help" ]; then
    echo "Usage: $0 <environment>"
    echo "Environment variables:"
    echo "  MASTER_DAYS=30 - how long to keep frontends from master"
    echo "  BRANCH_DAYS=7  - how long to keep frontends from branches"
    exit 1
fi

### Find old frontends based on hashes ###

rm -f /tmp/metadata-hashes.txt
for metadata_file in $(aws --profile "voltti-$ENVIRONMENT" s3 ls "s3://$bucket/frontend/metadata/" | awk '{ print $4 }' | grep -v "^$"); do
    if [[ $metadata_file = *.yaml ]] && [ "${#metadata_file}" = "45" ]; then
        echo "${metadata_file::-5}" >> /tmp/metadata-hashes.txt
    else
        echo "invalid file: $metadata_file"
    fi
done

sort /tmp/metadata-hashes.txt | uniq > /tmp/unique-metadata.txt
rm -f /tmp/metadata-hashes.txt

# shellcheck disable=SC2013
for metadata_hash in $(cat /tmp/unique-metadata.txt); do
    aws --profile "voltti-$ENVIRONMENT" s3 cp "s3://${bucket}/frontend/metadata/${metadata_hash}.yaml" /tmp/metadata.yaml
    echo "Checking - $metadata_hash"
    set +e
    python3 -c "
from datetime import datetime
import yaml
metadata = yaml.safe_load(open('/tmp/metadata.yaml'))
delta = (datetime.now().astimezone() - datetime.fromisoformat(metadata['updated'])).days
branch = metadata['branch']
if delta > ($MASTER_DAYS if branch == 'master' else $BRANCH_DAYS):
    print(f'is {delta} days old from {branch}')
    exit(10)
exit(20)
"
    exit_code="$?"
    set -e
    if [ "$exit_code" = "10" ]; then
        echo "Cleaning up $metadata_hash"
        for frontend in "${FRONTENDS[@]}"; do
            aws --profile "voltti-$ENVIRONMENT" s3 rm --recursive "s3://${bucket}/frontend/${frontend}/${metadata_hash}/"
        done
        aws --profile "voltti-$ENVIRONMENT" s3 rm "s3://${bucket}/frontend/metadata/${metadata_hash}.yaml"
    elif [ "$exit_code" != "20" ]; then
        echo "Unexpected exit code"
        exit 11
    fi
done
rm -f /tmp/metadata.yaml

### Find hashes without metadata ###

rm -f /tmp/installed-hashes.txt
for frontend in "${FRONTENDS[@]}"; do
    for hash in $(aws --profile "voltti-$ENVIRONMENT" s3 ls "s3://${bucket}/frontend/${frontend}/" |grep PRE | rev | cut -d' ' -f1 | rev); do
        if [[ $hash = */ ]] && [ "${#hash}" = "41" ]; then
            echo "${hash::-1}" >> /tmp/installed-hashes.txt
        else
            echo "invalid hash directory: $hash"
        fi
    done
done

sort /tmp/installed-hashes.txt | uniq  > /tmp/unique-installed.txt
rm -f /tmp/installed-hashes.txt

for hash in $(comm -23 /tmp/unique-installed.txt /tmp/unique-metadata.txt); do
    key="frontend/metadata/${hash}.yaml"
    if ! aws --profile "voltti-$ENVIRONMENT" s3api head-object --bucket "$bucket" --key "$key"; then
        echo "Empty metadata for ${hash} - deleting ..."
        for frontend in "${FRONTENDS[@]}"; do
            aws --profile "voltti-$ENVIRONMENT" s3 rm --recursive "s3://${bucket}/frontend/${frontend}/${hash}/"
        done
    else
        echo "Unexpected error - not expecting metadata file"
        exit 10
    fi
done

rm -f /tmp/unique-installed.txt
rm -f /tmp/unique-metadata.txt
