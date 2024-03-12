# SPDX-FileCopyrightText: 2017-2022 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

ARG FRONTEND_TAG=master

FROM ghcr.io/${GITHUB_REPOSITORY_OWNER:-espoon-voltti}/evaka/frontend-common-builder:${FRONTEND_TAG} as frontend

RUN apt-get update \
 && apt-get install -y curl \
 && rm -rf /var/lib/apt/lists/*

RUN yarn exec playwright install --with-deps

COPY ./playwright/bin/run-tests.sh /bin/

CMD ["/bin/run-tests.sh"]
