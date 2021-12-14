# SPDX-FileCopyrightText: 2017-2020 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

FROM evaka-base:latest

USER root
RUN apt-get update \
 && apt-get -y --no-install-recommends install \
       curl \
       openjdk-17-jre-headless \
 && rm -rf /var/lib/apt/lists/*

ARG DD_JAVA_AGENT_VERSION="0.91.0"
ARG DD_JAVA_AGENT_SHA256="52c30ef421366a2a5c3d9725bb53e80b5ae9feb272cd69760ecb31bf8b019ef8"

RUN curl -sSfo /opt/dd-java-agent.jar "https://repo1.maven.org/maven2/com/datadoghq/dd-java-agent/${DD_JAVA_AGENT_VERSION}/dd-java-agent-${DD_JAVA_AGENT_VERSION}.jar" \
 && echo "${DD_JAVA_AGENT_SHA256}  /opt/dd-java-agent.jar" | sha256sum -c -

ADD ./dd-jmxfetch/ /etc/jmxfetch/

USER evaka
WORKDIR /home/evaka

COPY --chown=evaka:evaka "entrypoint.sh" "entrypoint.sh"
COPY --chown=evaka:evaka "target/org/" "org/"
COPY --chown=evaka:evaka "target/META-INF" "META-INF/"
COPY --chown=evaka:evaka "target/BOOT-INF/lib" "BOOT-INF/lib/"
COPY --chown=evaka:evaka "target/BOOT-INF/classes" "BOOT-INF/classes/"

# Add build and commit environment variables and labels
# for tracing the image to the commit and build from which the image has been built.
ARG build=none
ARG commit=none
ENV APP_BUILD "$build"
ENV APP_COMMIT "$commit"
LABEL fi.espoo.build="$build" \
      fi.espoo.commit="$commit"

EXPOSE 8888

ENTRYPOINT ["./entrypoint.sh"]
