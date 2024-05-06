# syntax=docker/dockerfile:1.6.0
# SPDX-FileCopyrightText: 2017-2023 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

ARG NGINX_VERSION=1.26.0

FROM node:20.12.2-bookworm-slim AS builder
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
COPY --from=customizations . src/lib-customizations/${EVAKA_CUSTOMIZATIONS}

ARG ICONS=free
ARG SENTRY_PUBLISH_ENABLED="false"
ARG SENTRY_AUTH_TOKEN=""
ARG SENTRY_ORG="espoon-voltti"
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

RUN apt-get update \
 && apt-get -y dist-upgrade \
 && apt-get -y --no-install-recommends install ruby \
 && curl -sSfL https://github.com/espoon-voltti/s3-downloader/releases/download/v1.4.1/s3downloader-linux-amd64 \
       -o /bin/s3download \
 && chmod +x /bin/s3download \
 && echo "520ea232e83a7cefe2a87d4f2af8433e383a4351464e213b7dd3b78ca0dc200f  /bin/s3download" | sha256sum -c - \
 && rm -rf /var/lib/apt/lists/*


# https://docs.datadoghq.com/tracing/setup_overview/proxy_setup/?tab=nginx # update version using get_latest_release
ARG OPENTRACING_NGINX_VERSION=v0.31.0
ARG OPENTRACING_NGINX_SHA256="d3f12a433c884ddc957e538fd50313edc7f6d5276f9401cb6e732fb901ab7e8b"
ARG DD_OPENTRACING_CPP_VERSION=v1.3.6
ARG DD_OPENTRACING_CPP_SHA256="80bafd78b79c6fbd73509d7902f6504ecb3dad9d67e90107254bfa33d6f3532f"

RUN cd /tmp \
 && curl -sSfLO "https://github.com/opentracing-contrib/nginx-opentracing/releases/download/${OPENTRACING_NGINX_VERSION}/linux-amd64-nginx-${NGINX_VERSION}-ot16-ngx_http_module.so.tgz" \
 && echo "${OPENTRACING_NGINX_SHA256}  linux-amd64-nginx-${NGINX_VERSION}-ot16-ngx_http_module.so.tgz" | sha256sum -c - \
 && tar zxf "linux-amd64-nginx-${NGINX_VERSION}-ot16-ngx_http_module.so.tgz" -C /usr/lib/nginx/modules \
 && rm "linux-amd64-nginx-${NGINX_VERSION}-ot16-ngx_http_module.so.tgz" \
 && curl -sSfLO https://github.com/DataDog/dd-opentracing-cpp/releases/download/${DD_OPENTRACING_CPP_VERSION}/linux-amd64-libdd_opentracing_plugin.so.gz \
 && echo "${DD_OPENTRACING_CPP_SHA256}  linux-amd64-libdd_opentracing_plugin.so.gz" | sha256sum -c - \
 && gunzip linux-amd64-libdd_opentracing_plugin.so.gz -c > /usr/local/lib/libdd_opentracing_plugin.so \
 && rm linux-amd64-libdd_opentracing_plugin.so.gz

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
