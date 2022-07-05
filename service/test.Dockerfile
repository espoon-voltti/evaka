# SPDX-FileCopyrightText: 2017-2022 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

ARG BASE_IMAGE=evaka-service-builder:latest
FROM "${BASE_IMAGE}"

RUN ./gradlew --no-daemon lintKotlin
RUN ./gradlew --no-daemon codegenCheck
RUN ./circle-check-migrations.sh
RUN ./gradlew --no-daemon dependencyCheckUpdate
RUN ./gradlew --no-daemon dependencyCheckAnalyze
RUN ./gradlew --no-daemon test
RUN cd custom-ktlint-rules \
 && ./gradlew --no-daemon test
