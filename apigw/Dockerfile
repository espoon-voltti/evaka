# SPDX-FileCopyrightText: 2017-2020 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

FROM node:18.18.2-bookworm-slim AS base

WORKDIR /project

ARG CACHE_BUST=2023-02-08

ENV LC_ALL C.UTF-8
ENV LANG C.UTF-8
ENV LANGUAGE C.UTF-8
RUN apt-get update \
 && apt-get -y dist-upgrade \
 && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
      tzdata \
      ca-certificates \
      curl \
      unzip \
 && curl -sSfL https://github.com/espoon-voltti/s3-downloader/releases/download/v1.3.0/s3downloader-linux-amd64 \
       -o /bin/s3download \
 && chmod +x /bin/s3download \
 && echo "d0ee074cbc04c1a36fb8cee6f99d9ff591fee89ea38d34a328d0ee1acb039a48  /bin/s3download" | sha256sum -c - \
 && ln -fs /usr/share/zoneinfo/Europe/Helsinki /etc/localtime \
 && dpkg-reconfigure --frontend noninteractive tzdata \
 && rm -rf /var/lib/apt/lists/*

FROM base AS builder

COPY ./.yarn ./.yarn
COPY ./package.json ./yarn.lock ./.yarnrc.yml ./

RUN yarn install --immutable

COPY . .

RUN yarn build

FROM builder AS test

RUN yarn lint
RUN yarn test-ci

FROM base

ENV NODE_ENV production

WORKDIR /home/evaka

COPY --from=builder /project .

RUN yarn workspaces focus --production \
 && yarn cache clean --all

ARG build=none
ARG commit=none

ENV APP_BUILD "$build"
ENV APP_COMMIT "$commit"

LABEL fi.espoo.build="$build" \
      fi.espoo.commit="$commit"

ENTRYPOINT ["./entrypoint.sh"]
CMD ["node", "dist/index.js"]
