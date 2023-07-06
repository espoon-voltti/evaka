# SPDX-FileCopyrightText: 2017-2022 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

FROM registry.access.redhat.com/ubi9 as base

RUN dnf install -y java-17-openjdk-headless glibc-langpack-en jq \
 && dnf clean all


FROM base as maven-builder

WORKDIR /project/

RUN dnf install -y maven \
 && dnf clean all

FROM maven-builder AS builder-authenticator

ADD ./evaka-review-profile/pom.xml /project/evaka-review-profile/pom.xml

RUN cd /project/evaka-review-profile \
 && mvn --batch-mode dependency:go-offline dependency:resolve clean package

ADD ./evaka-review-profile/ /project/evaka-review-profile

RUN cd /project/evaka-review-profile \
 && mvn --batch-mode clean install


FROM maven-builder AS builder-logger

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


FROM base AS builder-keycloak

ENV KEYCLOAK_VERSION 21.1.2
ARG KEYCLOAK_DIST=https://github.com/keycloak/keycloak/releases/download/$KEYCLOAK_VERSION/keycloak-$KEYCLOAK_VERSION.tar.gz

RUN dnf install -y tar gzip \
 && dnf clean all

ADD $KEYCLOAK_DIST /tmp/keycloak/

RUN cd /tmp/keycloak \
 && tar -xvf /tmp/keycloak/keycloak-*.tar.gz \
 && rm /tmp/keycloak/keycloak-*.tar.gz \
 && mv /tmp/keycloak/keycloak-* /opt/keycloak \
 && mkdir -p /opt/keycloak/data \
 && chmod -R g+rwX /opt/keycloak


FROM base

ENV LANG en_US.UTF-8
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true
ENV KC_DB=postgres

RUN mkdir -p /opt/jboss/startup-scripts \
 && chown 1000:1000 /opt/jboss/startup-scripts \
 && mkdir -p /opt/keycloak/data/password-blacklists/ \
 && curl -sSf "https://raw.githubusercontent.com/danielmiessler/SecLists/2021.1/Passwords/xato-net-10-million-passwords-1000000.txt" \
      -o /opt/keycloak/data/password-blacklists/default.txt \
 && echo "424a3e03a17df0a2bc2b3ca749d81b04e79d59cb7aeec8876a5a3f308d0caf51  /opt/keycloak/data/password-blacklists/default.txt" | sha256sum -c - \
 && echo "keycloak:x:0:root" >> /etc/group \
 && echo "keycloak:x:1000:0:keycloak user:/opt/keycloak:/sbin/nologin" >> /etc/passwd

COPY --from=builder-keycloak --chown=1000:0 /opt/keycloak /opt/keycloak
COPY --from=builder-theme /work/evaka /opt/keycloak/themes/evaka
COPY --from=builder-authenticator /project/evaka-review-profile/target/evaka-review-profile.jar \
          /opt/keycloak/providers/
COPY --from=builder-logger /project/target/evaka-logging.jar \
          /opt/keycloak/providers/

RUN /opt/keycloak/bin/kc.sh build --http-relative-path=/auth --db=postgres

USER 1000

EXPOSE 8080
EXPOSE 8443

ENTRYPOINT [ "/opt/keycloak/bin/kc.sh" ]

CMD ["start", "--optimized", "--log-console-output=json"]

ARG build=none
ARG commit=none
ENV APP_BUILD="$build" \
    APP_COMMIT="$commit"
