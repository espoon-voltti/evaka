# syntax=docker/dockerfile:1.6.0
# SPDX-FileCopyrightText: 2017-2023 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

ARG KEYCLOAK_VERSION=24.0.5

## Package containers ##

FROM registry.access.redhat.com/ubi9 AS ubi-maven

RUN mkdir -p /mnt/rootfs \
 && dnf install \
     --installroot /mnt/rootfs maven-openjdk17 \
     --releasever 9 --setopt install_weak_deps=false \
     --nodocs -y \
 && dnf --installroot /mnt/rootfs clean all

## Base containers ##

FROM quay.io/keycloak/keycloak:${KEYCLOAK_VERSION} as base

USER root

FROM base as maven-builder

WORKDIR /project/

COPY --from=ubi-maven /mnt/rootfs /

## Builder containers ##

FROM maven-builder AS builder-authenticator

ADD ./evaka-review-profile/pom.xml /project/pom.xml

RUN mvn --batch-mode dependency:go-offline dependency:resolve clean package

ADD ./evaka-review-profile/ /project

RUN mvn --batch-mode clean install


FROM maven-builder AS builder-logger

ADD ./evaka-logging/pom.xml /project/pom.xml

RUN mvn --batch-mode dependency:go-offline dependency:resolve clean package

ADD ./evaka-logging/ /project/

RUN mvn --batch-mode clean install


FROM node:16 AS builder-theme

WORKDIR /work

COPY ./theme/package*.json /work/
RUN npm ci
COPY ./theme/ /work/

RUN npm run build

## KeyCloak container ##

FROM base

ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true
ENV KC_DB=postgres
ENV KC_HTTP_RELATIVE_PATH=/auth

ADD --chmod=0644 \
    --checksum=sha256:424a3e03a17df0a2bc2b3ca749d81b04e79d59cb7aeec8876a5a3f308d0caf51 \
    https://raw.githubusercontent.com/danielmiessler/SecLists/2023.1/Passwords/xato-net-10-million-passwords-1000000.txt \
    /opt/keycloak/data/password-blacklists/default.txt

COPY --from=builder-theme /work/evaka /opt/keycloak/themes/evaka
COPY --from=builder-authenticator /project/target/evaka-review-profile.jar \
          /opt/keycloak/providers/
COPY --from=builder-logger /project/target/evaka-logging.jar \
          /opt/keycloak/providers/

RUN /opt/keycloak/bin/kc.sh build

USER 1000

CMD ["start", "--optimized", "--log-console-output=json"]

ARG build=none
ARG commit=none
ENV APP_BUILD="$build" \
    APP_COMMIT="$commit"
