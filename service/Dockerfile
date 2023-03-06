# SPDX-FileCopyrightText: 2017-2020 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

FROM eclipse-temurin:18-jammy as base

LABEL maintainer="https://github.com/espoon-voltti/evaka"

ARG CACHE_BUST=2023-02-08

ENV LC_ALL C.UTF-8
ENV LANG C.UTF-8
ENV LANGUAGE C.UTF-8
RUN apt-get update \
 && apt-get -y dist-upgrade \
 && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
      tzdata \
      ca-certificates \
      curl \
      unzip \
 && ln -fs /usr/share/zoneinfo/Europe/Helsinki /etc/localtime \
 && dpkg-reconfigure --frontend noninteractive tzdata \
 && rm -rf /var/lib/apt/lists/*

FROM base as builder

WORKDIR /evaka

COPY ./service/certificates/ /certificates/

# We only add public certificates to truststore here, but keytool requires password, so using dummy password
RUN TRUSTSTORE_PASSWORD="public-certificates-only" /certificates/integraatiopalvelu.sh truststore

COPY ./espoo-customizations/ ./espoo-customizations/
COPY ./service/gradle/ ./service/gradle/
COPY ./service/custom-ktlint-rules/ ./service/custom-ktlint-rules/
COPY ./service/buildSrc ./service/buildSrc
COPY ./service/gradlew ./service/build.gradle.kts ./service/gradle.properties ./service/settings.gradle.kts ./service/
COPY ./service/vtjclient/build.gradle.kts ./service/vtjclient/build.gradle.kts
COPY ./service-lib/*.kts ./service-lib/
COPY ./evaka-bom/*.kts ./evaka-bom/
COPY ./service/sficlient/*.kts ./service/sficlient/

WORKDIR /evaka/service

RUN ./gradlew --no-daemon resolveDependencies

COPY ./service/sficlient/ ./sficlient/
COPY ./service/vtjclient/ ./vtjclient/
RUN ./gradlew --no-daemon :sficlient:wsdl2java :vtjclient:wsdl2java

RUN cd custom-ktlint-rules && ./gradlew dependencies

COPY . /evaka

# --offline is used to be sure that all dependencies are installed in previous steps
RUN ./gradlew --offline --no-daemon assemble compileIntegrationTestKotlin \
 && unzip -oq build/libs/evaka-service-boot.jar -d target

RUN cd custom-ktlint-rules \
 && ./gradlew --no-daemon assemble

FROM base

WORKDIR /evaka/service

RUN curl -sSL https://github.com/espoon-voltti/s3-downloader/releases/download/v1.3.1/s3downloader-linux-amd64 -o /usr/local/bin/s3download \
 && chmod +x /usr/local/bin/s3download

ARG DD_JAVA_AGENT_VERSION="1.8.0"
ARG DD_JAVA_AGENT_SHA256="85ccbbf73f867d10ab867ae02ad34f381d65e1f4523636a0b41d3aa46f98d584"

RUN curl -sSfo /opt/dd-java-agent.jar "https://repo1.maven.org/maven2/com/datadoghq/dd-java-agent/${DD_JAVA_AGENT_VERSION}/dd-java-agent-${DD_JAVA_AGENT_VERSION}.jar" \
 && echo "${DD_JAVA_AGENT_SHA256}  /opt/dd-java-agent.jar" | sha256sum -c -

ADD ./service/dd-jmxfetch/ /etc/jmxfetch/

COPY ./service/entrypoint.sh entrypoint.sh
ENTRYPOINT ["./entrypoint.sh"]

ENV USERNAME evaka
ENV HOME_DIR /home/${USERNAME}
ENV USER_ID 1000

RUN adduser ${USERNAME} --gecos "" -q --home ${HOME_DIR} --uid ${USER_ID} --disabled-password

COPY --from=builder /certificates/truststore/ /certificates/truststore/
COPY --from=builder /evaka/service/target/ .

USER ${USERNAME}

ARG build=none
ARG commit=none
ENV APP_BUILD "$build"
ENV APP_COMMIT "$commit"
LABEL fi.espoo.build="$build" \
      fi.espoo.commit="$commit"
