# SPDX-FileCopyrightText: 2017-2020 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

FROM evaka-base:latest

USER root
RUN set -eux \
    && apt-get update && apt-get -y --no-install-recommends install \
       curl \
       openjdk-11-jre-headless \
    && rm -rf /var/lib/apt/lists/*

USER evaka
WORKDIR /home/evaka

COPY --chown=evaka:evaka "entrypoint.sh" "entrypoint.sh"
RUN chmod +x "entrypoint.sh"
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
