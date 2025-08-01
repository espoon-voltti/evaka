# syntax=docker/dockerfile:1.17.1
# SPDX-FileCopyrightText: 2017-2023 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

ARG NGINX_VERSION=1.27.5

FROM node:22.17.1-bookworm-slim AS builder
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

COPY . .

ARG EVAKA_CUSTOMIZATIONS=espoo
COPY --from=customizations . src/lib-customizations/${EVAKA_CUSTOMIZATIONS}/

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

RUN yarn build

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
 && apt-get remove --auto-remove -y nginx-module-image-filter nginx-module-njs nginx-module-xslt \
 && curl -sSfL "https://github.com/hairyhenderson/gomplate/releases/download/${GOMPLATE_VERSION}/gomplate_linux-amd64" \
       -o /bin/gomplate \
 && chmod +x /bin/gomplate \
 && echo "${GOMPLATE_SHA256}  /bin/gomplate" | sha256sum -c - \
 && curl -sSfL https://github.com/espoon-voltti/s3-downloader/releases/download/${S3_DOWNLOADER_VERSION}/s3downloader-linux-amd64 \
       -o /bin/s3download \
 && chmod +x /bin/s3download \
 && echo "${S3_DOWNLOADER_SHA256}  /bin/s3download" | sha256sum -c - \
 && rm -rf /var/lib/apt/lists/*


# https://docs.datadoghq.com/tracing/setup_overview/proxy_setup/?tab=nginx # update version using get_latest_release
ARG OPENTRACING_NGINX_VERSION=v0.40.0
ARG OPENTRACING_NGINX_SHA256="62fe4230fba540d3a63451bbd6e706b465f65579bd8e32e199bb3fa23e6fe6db"
ARG DD_OPENTRACING_CPP_VERSION=v1.3.7
ARG DD_OPENTRACING_CPP_SHA256="df9a8097c3d6f6e5a96f96db0f3919b5b7b35df3d10c5fcc530707a878e2dd00"

RUN cd /tmp \
 && curl -sSfLO "https://github.com/opentracing-contrib/nginx-opentracing/releases/download/${OPENTRACING_NGINX_VERSION}/linux-amd64-nginx-${NGINX_VERSION}-ngx_http_module.so.tgz" \
 && echo "${OPENTRACING_NGINX_SHA256}  linux-amd64-nginx-${NGINX_VERSION}-ngx_http_module.so.tgz" | sha256sum -c - \
 && tar zxf "linux-amd64-nginx-${NGINX_VERSION}-ngx_http_module.so.tgz" -C /usr/lib/nginx/modules \
 && rm "linux-amd64-nginx-${NGINX_VERSION}-ngx_http_module.so.tgz" \
 && curl -sSfLO https://github.com/DataDog/dd-opentracing-cpp/releases/download/${DD_OPENTRACING_CPP_VERSION}/linux-amd64-libdd_opentracing_plugin.so.gz \
 && echo "${DD_OPENTRACING_CPP_SHA256}  linux-amd64-libdd_opentracing_plugin.so.gz" | sha256sum -c - \
 && gunzip linux-amd64-libdd_opentracing_plugin.so.gz -c > /usr/local/lib/libdd_opentracing_plugin.so \
 && rm linux-amd64-libdd_opentracing_plugin.so.gz \
 && apt-get remove --auto-remove -y curl

COPY ./proxy/files/bin/ /bin/
COPY ./proxy/files/internal/ /internal/
COPY ./proxy/files/etc/ /etc/

ENTRYPOINT ["/bin/proxy-entrypoint.sh"]
CMD ["nginx", "-g", "daemon off;"]

RUN mkdir /static
COPY --from=builder /project/dist/bundle/favicon.ico /static
COPY --from=builder /project/dist/bundle/assets /static/assets
COPY --from=builder /project/dist/bundle/employee /static/employee
COPY --from=builder /project/dist/bundle/src/citizen-frontend/index.html /static
COPY --from=builder /project/dist/bundle/src/employee-frontend/index.html /static/employee
COPY --from=builder /project/dist/bundle/src/employee-mobile-frontend/index.html /static/employee/mobile

COPY --from=builder /project/src/maintenance-page-frontend /static/maintenance-page

# Add build and commit environment variables and labels
# for tracing the image to the commit and build from which the image has been built.
ARG build=none
ARG commit=none
ENV APP_BUILD="$build" \
    APP_COMMIT="$commit"
LABEL fi.espoo.build="$build" \
      fi.espoo.commit="$commit"
