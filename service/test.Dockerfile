# SPDX-FileCopyrightText: 2017-2022 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

ARG BASE_IMAGE=evaka-service-builder:latest
FROM "${BASE_IMAGE}"

RUN ./circle-check-migrations.sh
RUN ./gradlew --no-daemon ktfmtCheck ktlintCheck codegenCheck test
