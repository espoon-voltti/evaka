# SPDX-FileCopyrightText: 2017-2020 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

FROM node:24.15.0-trixie-slim AS base
ARG CACHE_BUST

WORKDIR /project

ENV LC_ALL=C.UTF-8
ENV LANG=C.UTF-8
ENV LANGUAGE=C.UTF-8

ARG S3_DOWNLOADER_VERSION=v1.5.1
ARG S3_DOWNLOADER_SHA256="cb751589ca60749c42b65009671d6076435e7454b8980ee98884e296a1ec6cb8"

RUN apt-get update \
 && apt-get -y dist-upgrade \
 && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
      tzdata \
      ca-certificates \
      curl \
      unzip \
 && curl -sSfL https://github.com/espoon-voltti/s3-downloader/releases/download/${S3_DOWNLOADER_VERSION}/s3downloader-linux-amd64 \
       -o /bin/s3download \
 && echo "${S3_DOWNLOADER_SHA256}  /bin/s3download" | sha256sum -c - \
 && chmod +x /bin/s3download \
 && ln -fs /usr/share/zoneinfo/Europe/Helsinki /etc/localtime \
 && dpkg-reconfigure --frontend noninteractive tzdata \
 && apt-get remove --auto-remove -y curl \
 && rm -rf /var/lib/apt/lists/*

FROM base AS builder

COPY ./.yarn ./.yarn
COPY ./package.json ./yarn.lock ./.yarnrc.yml ./

RUN yarn install --immutable

COPY . .

FROM builder AS test

RUN yarn lint
RUN yarn type-check
RUN yarn test

FROM base

ENV NODE_ENV=production

RUN addgroup --gid 1001 evaka \
 && adduser --uid 1001 --gid 1001 --gecos "" -q --home /home/evaka --disabled-password evaka \
 && mkdir -p /config \
 && chown evaka:evaka /config \
 && ln -s /config /home/evaka/s3 # old config path for backwards compatibility

VOLUME /config

COPY --from=builder /project .

RUN yarn workspaces focus --production \
 && yarn cache clean --all

USER evaka

ARG build=none
ARG commit=none

ENV APP_BUILD="$build"
ENV APP_COMMIT="$commit"

LABEL fi.espoo.build="$build" \
      fi.espoo.commit="$commit"

ENTRYPOINT ["./entrypoint.sh"]
CMD ["node", "--loader", "dd-trace/loader-hook.mjs", "src/index.ts"]
