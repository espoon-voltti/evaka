# SPDX-FileCopyrightText: 2017-2021 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

ARG BASE_IMAGE=evaka-base:latest
FROM "${BASE_IMAGE}"

USER root
RUN export DEBIAN_FRONTEND=noninteractive \
 && apt-get update \
 && apt-get -y --no-install-recommends install \
      curl=7.81.* \
      gpg=2.2.* \
      gpg-agent=2.2.* \
 && curl -sL https://deb.nodesource.com/setup_18.x | bash - \
 && curl -sS https://dl.yarnpkg.com/debian/pubkey.gpg | tee /etc/apt/trusted.gpg.d/yarn.asc \
 && echo "deb https://dl.yarnpkg.com/debian/ stable main" | tee /etc/apt/sources.list.d/yarn.list \
 && apt-get update \
 && apt-get -y --no-install-recommends install \
      nodejs \
      yarn \
 && rm -rf /var/lib/apt/lists/*

USER evaka
