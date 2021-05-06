# SPDX-FileCopyrightText: 2017-2021 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

FROM nginx:1.19-alpine

LABEL maintainer="https://github.com/espoon-voltti/evaka"

ENV NGINX_ENV=local \
    TZ=UTC

RUN apk add --no-cache 'ruby=~2.7' \
 && curl -sSfL https://github.com/espoon-voltti/s3-downloader/releases/download/v1.3.0/s3downloader-linux-amd64 \
       -o /bin/s3download \
 && chmod +x /bin/s3download \
 && echo "d0ee074cbc04c1a36fb8cee6f99d9ff591fee89ea38d34a328d0ee1acb039a48  /bin/s3download" | sha256sum -c -

COPY ./files/ /
CMD ["/bin/proxy-start.sh"]

# Add build and commit environment variables and labels
# for tracing the image to the commit and build from which the image has been built.
ARG build=none
ARG commit=none
ENV APP_BUILD="$build" \
    APP_COMMIT="$commit"
LABEL fi.espoo.build="$build" \
      fi.espoo.commit="$commit"

