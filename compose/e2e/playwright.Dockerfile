# SPDX-FileCopyrightText: 2017-2022 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

ARG PLAYWRIGHT_VERSION=v1.39.0
ARG FRONTEND_TAG=master

FROM ghcr.io/espoon-voltti/evaka/frontend-common-builder:${FRONTEND_TAG} as frontend

FROM mcr.microsoft.com/playwright:${PLAYWRIGHT_VERSION}-focal

ENV NVM_DIR /usr/local/nvm

RUN mkdir -p /usr/local/nvm \
 && rm /etc/apt/sources.list.d/nodesource.list \
 && curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.5/install.sh | bash \
 && . "${NVM_DIR}/nvm.sh" \
 && nvm install 18.17 \
 && nvm ls


COPY ./playwright/bin/run-tests.sh /bin/

COPY --from=frontend /root/.yarn /root/.yarn

CMD ["/bin/run-tests.sh"]
