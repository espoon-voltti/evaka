# syntax=docker/dockerfile:1.23.0
# SPDX-FileCopyrightText: 2017-2026 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

ARG NGINX_VERSION=1.29.2

# ---------- Base ----------

FROM node:24.15.0-trixie-slim AS base
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

ARG SENTRY_PUBLISH_ENABLED="false"
ARG SENTRY_AUTH_TOKEN=""
ARG SENTRY_ORG="espoo-voltti"
ARG build=none
ARG commit=none

ENV SENTRY_PUBLISH_ENABLED="$SENTRY_PUBLISH_ENABLED"
ENV SENTRY_ORG="$SENTRY_ORG"
ENV SENTRY_AUTH_TOKEN="$SENTRY_AUTH_TOKEN"
ENV SENTRY_NO_PROGRESS_BAR="1"
ENV APP_BUILD="$build"
ENV APP_COMMIT="$commit"

# ---------- Multi-municipality builder ----------
#
# Run build for all customizations under src/lib-customizations/

FROM base AS builder-multi
RUN set -e; \
    mkdir -p /out/static; \
    for muni_dir in src/lib-customizations/*/; do \
      name=$(basename "$muni_dir"); \
      case "$name" in defaults) continue ;; esac; \
      icons_file="$muni_dir/icons"; \
      if [ ! -f "$icons_file" ]; then \
        echo "ERROR: $icons_file is missing" >&2; \
        exit 1; \
      fi; \
      icons=$(cat "$icons_file"); \
      echo "Building $name (icons=$icons)"; \
      EVAKA_CUSTOMIZATIONS="$name" ICONS="$icons" yarn build; \
      /project/assemble-static-bundle.sh "$name"; \
      rm -rf dist; \
    done

# ---------- Single-municipality builder ----------
#
# Run build for src/lib-customizations/$EVAKA_CUSTOMIZATIONS

FROM base AS builder-single
ARG EVAKA_CUSTOMIZATIONS=espoo
ARG ICONS=free

COPY --from=customizations . src/lib-customizations/${EVAKA_CUSTOMIZATIONS}/

ENV EVAKA_CUSTOMIZATIONS="$EVAKA_CUSTOMIZATIONS"
ENV ICONS="$ICONS"

RUN yarn build \
 && mkdir -p /out/static \
 && /project/assemble-static-bundle.sh "${EVAKA_CUSTOMIZATIONS}"

# ---------- Shared runtime base ----------

FROM nginx:${NGINX_VERSION}-otel AS runtime-base
ARG CACHE_BUST

LABEL maintainer="https://github.com/espoon-voltti/evaka"

ENV NGINX_ENV=local \
    TZ=UTC

# https://github.com/hairyhenderson/gomplate
ARG GOMPLATE_VERSION=v4.0.1
ARG GOMPLATE_SHA256="5aeae23cbc51e243967162a62edba0098218668a09cdaa6d6de1b228c070990f"

RUN apt-get update \
 && apt-get -y dist-upgrade \
 && apt-get remove --auto-remove -y nginx-module-image-filter nginx-module-njs nginx-module-xslt \
 && curl -sSfL "https://github.com/hairyhenderson/gomplate/releases/download/${GOMPLATE_VERSION}/gomplate_linux-amd64" \
       -o /bin/gomplate \
 && chmod +x /bin/gomplate \
 && echo "${GOMPLATE_SHA256}  /bin/gomplate" | sha256sum -c - \
 && apt-get remove --auto-remove -y curl \
 && rm -rf /var/lib/apt/lists/* \
 && rm /etc/nginx/conf.d/default.conf /etc/nginx/nginx.conf # prevent accidental usage of default configs

COPY ./proxy/files/bin/ /bin/
COPY ./proxy/files/web/ /web/
COPY ./proxy/files/etc/ /etc/

RUN mkdir -p /nginx /internal && chown nginx:nginx /nginx /internal
RUN mkdir /static

VOLUME /nginx
VOLUME /internal

ARG build=none
ARG commit=none
ENV APP_BUILD="$build" \
    APP_COMMIT="$commit"
LABEL fi.espoo.build="$build" \
      fi.espoo.commit="$commit"

ENTRYPOINT ["/bin/proxy-entrypoint.sh"]
CMD ["nginx", "-c", "/nginx/config/nginx.conf", "-g", "daemon off;"]

# ---------- Multi-municipality runtime ----------

FROM runtime-base AS runtime-multi
COPY --from=builder-multi /out/static /static
USER nginx

# ---------- Single-municipality runtime ----------

FROM runtime-base AS runtime-single
ARG EVAKA_CUSTOMIZATIONS=espoo
ENV EVAKA_MUNICIPALITY=${EVAKA_CUSTOMIZATIONS}
COPY --from=builder-single /out/static /static
USER nginx
