# SPDX-FileCopyrightText: 2017-2025 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

FROM node:22.14.0-bookworm-slim AS base

WORKDIR /project

ENV LC_ALL=C.UTF-8
ENV LANG=C.UTF-8
ENV LANGUAGE=C.UTF-8
RUN apt-get update \
 && apt-get -y dist-upgrade \
 && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
      tzdata \
      ca-certificates \
 && ln -fs /usr/share/zoneinfo/Europe/Helsinki /etc/localtime \
 && dpkg-reconfigure --frontend noninteractive tzdata \
 && rm -rf /var/lib/apt/lists/*

FROM base AS builder

COPY ./package.json ./package-lock.json ./

RUN npm ci

COPY . .

RUN npm run build

FROM base

COPY --from=builder /project .

RUN npm ci --include=prod && npm cache clean --force

EXPOSE 9090

ENTRYPOINT ["node"]
CMD ["--enable-source-maps", "dist/index.js"]
