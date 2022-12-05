# SPDX-FileCopyrightText: 2017-2022 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

FROM maven:3.8.4-openjdk-17-slim AS builder-authenticator

WORKDIR /project/

ADD ./evaka-review-profile/pom.xml /project/evaka-review-profile/pom.xml

RUN cd /project/evaka-review-profile \
 && mvn --batch-mode dependency:go-offline dependency:resolve clean package

ADD ./evaka-review-profile/ /project/evaka-review-profile

RUN cd /project/evaka-review-profile \
 && mvn --batch-mode clean install

FROM maven:3.8.4-openjdk-17-slim AS builder-logger

WORKDIR /project/

ADD ./evaka-logging/pom.xml /project/pom.xml

RUN mvn --batch-mode dependency:go-offline dependency:resolve clean package

ADD ./evaka-logging/ /project/

RUN mvn --batch-mode clean install

FROM node:16 AS builder-theme

WORKDIR /work

COPY ./theme/package*.json /work/
RUN npm install
COPY ./theme/ /work/

RUN npm run build

FROM quay.io/keycloak/keycloak:20.0.1 AS keycloak

USER root

ARG CACHE_BUST=2022-12-05

RUN microdnf upgrade -y \
 && microdnf install -y jq \
 && microdnf clean all \
 && mkdir -p /opt/jboss/startup-scripts \
 && chown 1000:1000 /opt/jboss/startup-scripts

RUN mkdir -p /opt/keycloak/data/password-blacklists/ \
  && curl -sSf "https://raw.githubusercontent.com/danielmiessler/SecLists/2021.1/Passwords/xato-net-10-million-passwords-1000000.txt" \
     -o /opt/keycloak/data/password-blacklists/default.txt \
  && echo "424a3e03a17df0a2bc2b3ca749d81b04e79d59cb7aeec8876a5a3f308d0caf51  /opt/keycloak/data/password-blacklists/default.txt" | sha256sum -c -

FROM keycloak

COPY --from=builder-theme /work/evaka /opt/keycloak/themes/evaka

COPY --from=builder-authenticator /project/evaka-review-profile/target/evaka-review-profile.jar \
          /opt/keycloak/providers/

COPY --from=builder-logger /project/target/evaka-logging.jar \
          /opt/keycloak/providers/

ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true
ENV KC_DB=postgres

RUN /opt/keycloak/bin/kc.sh build --http-relative-path=/auth --db=postgres

USER 1000

CMD ["start", "--optimized", "--log-console-output=json"]

ARG build=none
ARG commit=none
ENV APP_BUILD="$build" \
    APP_COMMIT="$commit"
