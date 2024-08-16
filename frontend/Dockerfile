# syntax=docker/dockerfile:1.7-labs
# SPDX-FileCopyrightText: 2017-2023 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

ARG NGINX_VERSION=1.26.2-otel

FROM node:20.17.0-bookworm-slim AS builder
ARG CACHE_BUST

USER root

WORKDIR /project

RUN apt-get update \
 && apt-get -y dist-upgrade \
 && apt-get -y install ca-certificates \
 && rm -rf /var/lib/apt/lists/*

COPY ./.yarn ./.yarn
COPY ./package.json ./yarn.lock ./.yarnrc.yml ./
RUN yarn install --immutable

COPY --exclude=proxy/ . .

ARG EVAKA_CUSTOMIZATIONS=espoo
COPY --from=customizations . src/lib-customizations/${EVAKA_CUSTOMIZATIONS}

ARG ICONS=free
ARG SENTRY_PUBLISH_ENABLED="false"
ARG SENTRY_AUTH_TOKEN=""
ARG SENTRY_ORG="espoo-voltti"
ARG build=none
ARG commit=none

ENV EVAKA_CUSTOMIZATIONS="$EVAKA_CUSTOMIZATIONS"
ENV ICONS="$ICONS"
ENV SENTRY_PUBLISH_ENABLED="$SENTRY_PUBLISH_ENABLED"
ENV SENTRY_ORG="$SENTRY_ORG"
ENV SENTRY_AUTH_TOKEN="$SENTRY_AUTH_TOKEN"
ENV SENTRY_NO_PROGRESS_BAR="1"
ENV APP_BUILD="$build"
ENV APP_COMMIT="$commit"

RUN export NODE_OPTIONS="--max-old-space-size=4096" \
 && yarn build

FROM nginx:${NGINX_VERSION}
ARG CACHE_BUST

LABEL maintainer="https://github.com/espoon-voltti/evaka"

ENV NGINX_ENV=local \
    TZ=UTC

# https://github.com/hairyhenderson/gomplate
ARG GOMPLATE_VERSION=v4.0.1
ARG GOMPLATE_SHA256="5aeae23cbc51e243967162a62edba0098218668a09cdaa6d6de1b228c070990f"
# https://github.com/espoon-voltti/s3-downloader/
ARG S3_DOWNLOADER_VERSION=v1.4.1
ARG S3_DOWNLOADER_SHA256="520ea232e83a7cefe2a87d4f2af8433e383a4351464e213b7dd3b78ca0dc200f"

RUN apt-get update \
 && apt-get -y dist-upgrade \
 && curl -sSfL "https://github.com/hairyhenderson/gomplate/releases/download/${GOMPLATE_VERSION}/gomplate_linux-amd64" \
       -o /bin/gomplate \
 && chmod +x /bin/gomplate \
 && echo "${GOMPLATE_SHA256}  /bin/gomplate" | sha256sum -c - \
 && curl -sSfL https://github.com/espoon-voltti/s3-downloader/releases/download/${S3_DOWNLOADER_VERSION}/s3downloader-linux-amd64 \
       -o /bin/s3download \
 && chmod +x /bin/s3download \
 && echo "${S3_DOWNLOADER_SHA256}  /bin/s3download" | sha256sum -c - \
 && rm -rf /var/lib/apt/lists/*

COPY ./proxy/files/bin/ /bin/
COPY ./proxy/files/internal/ /internal/
COPY ./proxy/files/etc/ /etc/

ENTRYPOINT ["/bin/proxy-entrypoint.sh"]
CMD ["nginx", "-g", "daemon off;"]

COPY --from=builder /project/dist/bundle/citizen-frontend /static/
COPY --from=builder /project/dist/bundle/employee-frontend /static/employee
COPY --from=builder /project/dist/bundle/employee-mobile-frontend /static/employee/mobile
COPY --from=builder /project/src/maintenance-page-frontend /static/maintenance-page

# Add build and commit environment variables and labels
# for tracing the image to the commit and build from which the image has been built.
ARG build=none
ARG commit=none
ENV APP_BUILD="$build" \
    APP_COMMIT="$commit"
LABEL fi.espoo.build="$build" \
      fi.espoo.commit="$commit"
