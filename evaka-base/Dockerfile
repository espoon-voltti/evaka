# SPDX-FileCopyrightText: 2017-2021 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

FROM ubuntu:20.04

LABEL maintainer="https://github.com/espoon-voltti/evaka"

# Use bash instead of dash
SHELL ["/bin/bash", "-c"]

# Update time zone information
ENV LC_ALL=C.UTF-8 \
    LANG=C.UTF-8 \
    LANGUAGE=C.UTF-8

RUN set -euxo pipefail \
 && apt-get update \
 && apt-get -y dist-upgrade \
 && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        tzdata \
        ca-certificates \
 && ln -fs /usr/share/zoneinfo/Europe/Helsinki /etc/localtime \
 && dpkg-reconfigure --frontend noninteractive tzdata \
 && rm -rf /var/lib/apt/lists/*

# Set evaka username
ENV USERNAME=evaka \
    HOME_DIR=/home/evaka \
    USER_ID=1000

# Create a new user named evaka which should be used to run any software.
RUN adduser ${USERNAME} --gecos "" -q --home ${HOME_DIR} --uid ${USER_ID} --disabled-password

USER ${USERNAME}

# Used by all services for downloading keys from S3
COPY ./s3downloader-linux-amd64 /usr/local/bin/s3download
