# SPDX-FileCopyrightText: 2017-2020 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

ARG BASE_IMAGE=library/postgres
ARG BASE_IMAGE_VERSION=10.13-alpine

FROM ${BASE_IMAGE}:${BASE_IMAGE_VERSION}

# Extension whitelisting to simulate the RDS behaviour
# Build https://github.com/dimitri/pgextwlist package from source, transfer it to target installation
# TODO To shrink the image, build whitelisting as multi-stage build
RUN apk add --update alpine-sdk
RUN git clone https://github.com/dimitri/pgextwlist.git /tmp/pgextwlist
WORKDIR /tmp/pgextwlist
RUN make && make install
RUN mkdir `pg_config --pkglibdir`/plugins && \
    install pgextwlist.so `pg_config --pkglibdir`/plugins/pgextwlist.so
ADD postgresql.conf /var/lib/postgresql/

# Add the initialisation scripts
COPY *.sql /docker-entrypoint-initdb.d/

# Force the modified postgres configuration ito use
CMD ["-c", "config_file=/var/lib/postgresql/postgresql.conf"]
