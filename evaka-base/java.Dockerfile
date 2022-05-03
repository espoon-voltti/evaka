# SPDX-FileCopyrightText: 2017-2021 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

ARG BASE_IMAGE=evaka-base:latest
FROM "${BASE_IMAGE}"

WORKDIR /evaka

USER root

ARG CACHE_BUST=2022-05-03

RUN apt-get update \
 && apt-get -y --no-install-recommends install \
       curl \
       unzip \
       openjdk-17-jre-headless \
 && rm -rf /var/lib/apt/lists/*

ARG DD_JAVA_AGENT_VERSION="0.100.0"
ARG DD_JAVA_AGENT_SHA256="2b7d1266ea7ac8fb164eaba6bc59c36d5c9fdb3358128acd52d1947cac13e667"

RUN curl -sSfo /opt/dd-java-agent.jar "https://repo1.maven.org/maven2/com/datadoghq/dd-java-agent/${DD_JAVA_AGENT_VERSION}/dd-java-agent-${DD_JAVA_AGENT_VERSION}.jar" \
 && echo "${DD_JAVA_AGENT_SHA256}  /opt/dd-java-agent.jar" | sha256sum -c -

ADD ./dd-jmxfetch/ /etc/jmxfetch/
