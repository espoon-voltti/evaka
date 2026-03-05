# SPDX-FileCopyrightText: 2017-2026 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

FROM mcr.microsoft.com/devcontainers/base:ubuntu

RUN apt-get update \
    && apt-get install -y --no-install-recommends locales netcat-openbsd docker.io docker-compose-v2 \
    && sed -i 's/# en_US.UTF-8/en_US.UTF-8/' /etc/locale.gen \
    && locale-gen en_US.UTF-8 fi_FI.UTF-8 \
    && rm -rf /var/lib/apt/lists/*

ENV LANG=en_US.UTF-8
ENV LANGUAGE=en_US:en
ENV LC_ALL=en_US.UTF-8

ARG USER_NAME
ARG USER_UID
ARG USER_GID
ARG USER_HOME

RUN SANITIZED_USER=$(echo "${USER_NAME}" | sed 's/[^a-z0-9_-]/-/g') \
    && groupadd -f -g ${USER_GID} ${SANITIZED_USER} \
    && useradd -m -d ${USER_HOME} -u ${USER_UID} -g ${USER_GID} -s /bin/bash ${SANITIZED_USER} \
    && echo "${SANITIZED_USER} ALL=(ALL) NOPASSWD:ALL" > /etc/sudoers.d/${SANITIZED_USER} \
    && usermod -aG docker ${SANITIZED_USER}

USER ${USER_UID}
