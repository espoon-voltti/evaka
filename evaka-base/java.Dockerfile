# SPDX-FileCopyrightText: 2017-2021 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

ARG BASE_IMAGE=evaka-base:latest
FROM "${BASE_IMAGE}"

WORKDIR /evaka

USER root

RUN apt-get update \
 && apt-get -y --no-install-recommends install \
       curl \
       unzip \
       openjdk-17-jre-headless \
 && rm -rf /var/lib/apt/lists/*

ARG DD_JAVA_AGENT_VERSION="1.4.0"
ARG DD_JAVA_AGENT_SHA256="a8fcc0b614d47872a98cf1fe3ab099ec626935b5d5027a873ca3e47ca2ed3a65"

RUN curl -sSfo /opt/dd-java-agent.jar "https://repo1.maven.org/maven2/com/datadoghq/dd-java-agent/${DD_JAVA_AGENT_VERSION}/dd-java-agent-${DD_JAVA_AGENT_VERSION}.jar" \
 && echo "${DD_JAVA_AGENT_SHA256}  /opt/dd-java-agent.jar" | sha256sum -c -

ADD ./dd-jmxfetch/ /etc/jmxfetch/
