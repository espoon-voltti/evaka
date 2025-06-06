# SPDX-FileCopyrightText: 2017-2020 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

FROM azul/zulu-openjdk:21-latest AS builder
ARG CACHE_BUST

ENV LC_ALL=C.UTF-8
ENV LANG=C.UTF-8
ENV LANGUAGE=C.UTF-8
RUN apt-get update \
 && apt-get -y dist-upgrade \
 && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
      tzdata \
      ca-certificates \
      unzip \
 && ln -fs /usr/share/zoneinfo/Europe/Helsinki /etc/localtime \
 && dpkg-reconfigure --frontend noninteractive tzdata \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /evaka

COPY ./service/gradle/ ./service/gradle/
COPY ./service/custom-ktlint-rules/ ./service/custom-ktlint-rules/
COPY ./service/gradlew ./service/build.gradle.kts ./service/gradle.properties ./service/settings.gradle.kts ./service/
COPY ./service/vtjclient/build.gradle.kts ./service/vtjclient/build.gradle.kts
COPY ./service/service-lib/*.kts ./service/service-lib/
COPY ./service/evaka-bom/*.kts ./service/evaka-bom/
COPY ./service/sarmamodel/build.gradle.kts ./service/sarmamodel/

WORKDIR /evaka/service

RUN ./gradlew --no-daemon resolveDependencies

COPY ./service/vtjclient/ ./vtjclient/
COPY ./service/sarmamodel/ ./sarmamodel/
RUN ./gradlew --no-daemon :vtjclient:wsdl2java :sarmamodel:build

COPY . /evaka

# --offline is used to be sure that all dependencies are installed in previous steps
RUN ./gradlew --offline --no-daemon assemble compileIntegrationTestKotlin copyDownloadOnlyDeps \
 && unzip -oq build/libs/evaka-service-boot.jar -d target

FROM azul/zulu-openjdk:21-jre-headless-latest
ARG CACHE_BUST

LABEL maintainer="https://github.com/espoon-voltti/evaka"

ENV LC_ALL=C.UTF-8
ENV LANG=C.UTF-8
ENV LANGUAGE=C.UTF-8
RUN apt-get update \
 && apt-get -y dist-upgrade \
 && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
      tzdata \
      ca-certificates \
      curl \
 && ln -fs /usr/share/zoneinfo/Europe/Helsinki /etc/localtime \
 && dpkg-reconfigure --frontend noninteractive tzdata \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /evaka/service

RUN curl -sSL https://github.com/espoon-voltti/s3-downloader/releases/download/v1.4.1/s3downloader-linux-amd64 -o /usr/local/bin/s3download \
 && echo "520ea232e83a7cefe2a87d4f2af8433e383a4351464e213b7dd3b78ca0dc200f  /usr/local/bin/s3download" | sha256sum -c - \
 && chmod +x /usr/local/bin/s3download

COPY --from=builder /evaka/service/build/download-only/dd-java-agent.jar /opt/dd-java-agent.jar
COPY ./service/dd-jmxfetch/ /etc/jmxfetch/

COPY ./service/entrypoint.sh entrypoint.sh
ENTRYPOINT ["./entrypoint.sh"]

ENV USERNAME=evaka
ENV HOME_DIR=/home/${USERNAME}
ENV USER_ID=1000

RUN adduser ${USERNAME} --gecos "" -q --home ${HOME_DIR} --uid ${USER_ID} --disabled-password

COPY --from=builder /evaka/service/target/ .

USER ${USERNAME}

ARG build=none
ARG commit=none
ENV APP_BUILD="$build"
ENV APP_COMMIT="$commit"
LABEL fi.espoo.build="$build" \
      fi.espoo.commit="$commit"
