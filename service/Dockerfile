# SPDX-FileCopyrightText: 2017-2020 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

ARG BASE_IMAGE=evaka-java:latest
FROM "${BASE_IMAGE}" as builder

COPY ./service/gradle/ ./service/gradle/
COPY ./service/custom-ktlint-rules/ ./service/custom-ktlint-rules/
COPY ./service/buildSrc ./service/buildSrc
COPY ./service/gradlew ./service/build.gradle.kts ./service/gradle.properties ./service/settings.gradle.kts ./service/
COPY ./service/vtjclient/build.gradle.kts ./service/vtjclient/build.gradle.kts
COPY ./service-lib/*.kts ./service-lib/
COPY ./evaka-bom/*.kts ./evaka-bom/
COPY ./service/sficlient/*.kts ./service/sficlient/

WORKDIR /evaka/service

RUN ./gradlew --no-daemon dependencies :service-lib:dependencies :evaka-bom:dependencies :vtjclient:dependencies :sficlient:dependencies

COPY ./service/sficlient/ ./sficlient/
COPY ./service/vtjclient/ ./vtjclient/
RUN ./gradlew --no-daemon :sficlient:wsdl2java :vtjclient:wsdl2java

RUN cd custom-ktlint-rules && ./gradlew dependencies

COPY . /evaka

# --offline is used to be sure that all dependencies are installed in previous steps
RUN ./gradlew --offline --no-daemon assemble \
 && unzip -oq build/libs/evaka-service-boot.jar -d target

RUN cd custom-ktlint-rules \
 && ./gradlew --no-daemon assemble

FROM "${BASE_IMAGE}"

WORKDIR /evaka/service

COPY ./service/entrypoint.sh entrypoint.sh
ENTRYPOINT ["./entrypoint.sh"]

COPY --from=builder /evaka/service/target/ .

USER evaka

ARG build=none
ARG commit=none
ENV APP_BUILD "$build"
ENV APP_COMMIT "$commit"
LABEL fi.espoo.build="$build" \
      fi.espoo.commit="$commit"
