# SPDX-FileCopyrightText: 2017-2024 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

FROM azul/zulu-openjdk-alpine:21-latest AS builder
WORKDIR /evaka

COPY gradle/ ./gradle
COPY gradlew .
RUN ./gradlew --no-daemon --version

COPY gradle.properties *.kts .
COPY src/ ./src
RUN ./gradlew --no-daemon assemble


FROM azul/zulu-openjdk-distroless:21-latest
WORKDIR /evaka

COPY --from=builder /evaka/build/libs/dummy-suomifi-all.jar .

EXPOSE 9000
CMD ["-jar", "dummy-suomifi-all.jar"]
