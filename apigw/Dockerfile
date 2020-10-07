# SPDX-FileCopyrightText: 2017-2020 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

FROM evaka-base:latest

ARG DEBIAN_FRONTEND=noninteractive
ARG NODEJS_PACKAGE=nodejs_12.18.4-deb-1nodesource1_amd64.deb

USER root
RUN set -euxo pipefail \
    && apt-get update && apt-get -y --no-install-recommends install \
       curl=7.68.* \
       gnupg2=2.2.* \
    && curl -O "https://deb.nodesource.com/node_12.x/pool/main/n/nodejs/${NODEJS_PACKAGE}" \
    && apt-get -y --no-install-recommends install "./${NODEJS_PACKAGE}" \
    && rm "$NODEJS_PACKAGE" \
    && curl -sS https://dl.yarnpkg.com/debian/pubkey.gpg | apt-key add - \
    && echo "deb https://dl.yarnpkg.com/debian/ stable main" | tee /etc/apt/sources.list.d/yarn.list \
    && apt-get update && apt-get -y --no-install-recommends install \
       yarn=1.22.* \
    && rm -rf /var/lib/apt/lists/*

USER evaka
WORKDIR /home/evaka

ENV NODE_ENV production
COPY --chown=evaka:evaka . .
RUN yarn install --production --frozen-lockfile && \
    yarn cache clean

# Add build and commit environment variables and labels
# for tracing the image to the commit and build from which the image has been built.
ARG build=none
ARG commit=none
ENV APP_BUILD "$build"
ENV APP_COMMIT "$commit"
LABEL fi.espoo.build="$build" \
      fi.espoo.commit="$commit"

ENTRYPOINT ["./entrypoint.sh"]
CMD ["node", "dist/index.js"]
