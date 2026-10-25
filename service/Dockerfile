# SPDX-FileCopyrightText: 2017-2020 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

FROM azul/zulu-openjdk:25-latest AS assemble
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

# bin/service-deps-layer-inputs.sh finds the inputs of the resolveDependencies layer from these
# COPY instructions. It does not read continuation lines, so keep each COPY on one line.
COPY ./service/gradle/ ./service/gradle/
COPY ./service/custom-ktlint-rules/ ./service/custom-ktlint-rules/
COPY ./service/gradlew ./service/build.gradle.kts ./service/gradle.properties ./service/settings.gradle.kts ./service/
COPY ./service/vtjclient/build.gradle.kts ./service/vtjclient/build.gradle.kts
COPY ./service/service-lib/*.kts ./service/service-lib/
COPY ./service/evaka-bom/*.kts ./service/evaka-bom/
COPY ./service/codegen/build.gradle.kts ./service/codegen/

WORKDIR /evaka/service

# The cache mount lets CI carry downloads over from earlier builds. A cache mount is not part of
# the image, so the dependencies are also copied into the image: the --offline builds below and
# the users of the builder image do not mount the cache.
RUN --mount=type=cache,id=evaka-gradle-deps,target=/gradle-deps,sharing=locked \
    mkdir -p /gradle-deps/caches/modules-2 /gradle-deps/wrapper/dists /root/.gradle/caches /root/.gradle/wrapper \
 && cp -a /gradle-deps/caches/modules-2 /root/.gradle/caches/ \
 && cp -a /gradle-deps/wrapper/dists /root/.gradle/wrapper/ \
 && ./gradlew --no-daemon resolveDependencies \
 && cp -a -u /root/.gradle/caches/modules-2 /gradle-deps/caches/ \
 && cp -a -u /root/.gradle/wrapper/dists /gradle-deps/wrapper/

COPY ./service/vtjclient/ ./vtjclient/
RUN ./gradlew --no-daemon :vtjclient:wsdl2java

COPY ./service/ /evaka/service/

# --offline is used to be sure that all dependencies are installed in previous steps
#
# CI saves the build cache after every build. To keep it small, entries that were not read or
# written in the last hour are deleted. Gradle does not mark the entries it reads, so file access
# times are used instead: an access time of 0 makes the kernel (relatime) update it on the next read.
RUN --mount=type=cache,id=evaka-gradle-build-cache-assemble,target=/root/.gradle/caches/build-cache-1,sharing=locked \
    find /root/.gradle/caches/build-cache-1 -type f -amin +60 -exec touch -a -d @0 {} + \
 && ./gradlew --offline --no-daemon assemble copyDownloadOnlyDeps \
 && find /root/.gradle/caches/build-cache-1 -type f -amin +60 -delete \
 && unzip -oq build/libs/evaka-service-boot.jar -d target

FROM assemble AS builder

# This stage has its own build cache because the pruning in the assemble stage would delete the
# entries of this stage.
RUN --mount=type=cache,id=evaka-gradle-build-cache-tests,target=/root/.gradle/caches/build-cache-1,sharing=locked \
    find /root/.gradle/caches/build-cache-1 -type f -amin +60 -exec touch -a -d @0 {} + \
 && ./gradlew --offline --no-daemon compileIntegrationTestKotlin \
 && find /root/.gradle/caches/build-cache-1 -type f -amin +60 -delete

# codegenCheck in test.Dockerfile compares the generated API clients against these sources
COPY ./frontend/src/ /evaka/frontend/src/

FROM azul/zulu-openjdk:25-jre-headless-latest
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

ARG S3_DOWNLOADER_VERSION=v1.6.0
ARG S3_DOWNLOADER_SHA256="f69116f2e2eeb78fa6297e053ed7f311a1bf120c0ff0ea3c966335223f94a5e9"

RUN curl -sSL https://github.com/espoon-voltti/s3-downloader/releases/download/${S3_DOWNLOADER_VERSION}/s3downloader-linux-amd64 -o /usr/local/bin/s3download \
 && echo "${S3_DOWNLOADER_SHA256}  /usr/local/bin/s3download" | sha256sum -c - \
 && chmod +x /usr/local/bin/s3download

COPY --chmod=0644 --from=assemble /evaka/service/build/download-only/dd-java-agent.jar /opt/dd-java-agent.jar
COPY ./service/dd-jmxfetch/ /etc/jmxfetch/

COPY ./service/entrypoint.sh entrypoint.sh
ENTRYPOINT ["./entrypoint.sh"]

ENV USERNAME=evaka
ENV HOME_DIR=/home/${USERNAME}
ENV USER_ID=2000
ENV GROUP_ID=2000

RUN addgroup --gid ${GROUP_ID} ${USERNAME} \
 && adduser --uid ${USER_ID} --gid ${GROUP_ID} --gecos "" -q --home ${HOME_DIR} --disabled-password ${USERNAME} \
 && mkdir -p /config \
 && chown ${USERNAME}:${USERNAME} /config \
 && ln -s /config ${HOME_DIR}/s3 # old config path for backwards compatibility

VOLUME /config
VOLUME /tmp

COPY --from=assemble /evaka/service/target/ .

USER ${USERNAME}

ARG build=none
ARG commit=none
ENV APP_BUILD="$build"
ENV APP_COMMIT="$commit"
LABEL fi.espoo.build="$build" \
      fi.espoo.commit="$commit"
