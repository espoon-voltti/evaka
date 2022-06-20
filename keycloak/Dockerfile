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

FROM quay.io/keycloak/keycloak:18.0.1-legacy AS keycloak

CMD ["--server-config=standalone.xml", "-b", "0.0.0.0"]

USER root

ARG CACHE_BUST=2022-06-14

RUN microdnf upgrade -y \
 && microdnf install -y jq \
 && microdnf clean all \
 && mkdir -p /opt/jboss/startup-scripts \
 && chown 1000:1000 /opt/jboss/startup-scripts

USER 1000

RUN mkdir -p /opt/jboss/keycloak/standalone/data/password-blacklists/ \
 && curl -sSf "https://raw.githubusercontent.com/danielmiessler/SecLists/2021.1/Passwords/xato-net-10-million-passwords-1000000.txt" \
      -o /opt/jboss/keycloak/standalone/data/password-blacklists/default.txt \
 && echo "424a3e03a17df0a2bc2b3ca749d81b04e79d59cb7aeec8876a5a3f308d0caf51  /opt/jboss/keycloak/standalone/data/password-blacklists/default.txt" | sha256sum -c -

ADD ./bin/* /bin/
ADD ./cli/ /opt/jboss/startup-scripts/
ADD ./logging.properties.template /opt/jboss/keycloak/standalone/configuration/logging.properties.template

ENTRYPOINT ["/bin/entrypoint.sh"]

FROM keycloak

COPY --from=builder-theme /work/evaka /opt/jboss/keycloak/themes/evaka

COPY --from=builder-authenticator /project/evaka-review-profile/target/evaka-review-profile.jar \
          /opt/jboss/keycloak/standalone/deployments/

COPY --from=builder-logger /project/target/evaka-logging.jar \
          /opt/jboss/keycloak/standalone/deployments/

ARG build=none
ARG commit=none
ENV APP_BUILD="$build" \
    APP_COMMIT="$commit"
