# SPDX-FileCopyrightText: 2017-2023 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

ARG BASE_IMAGE=library/postgres
ARG BASE_IMAGE_VERSION=16-alpine3.19

FROM ${BASE_IMAGE}:${BASE_IMAGE_VERSION} AS ext_builder
# Extension whitelisting to simulate the RDS behaviour
# Build https://github.com/dimitri/pgextwlist package from source, transfer it to target installation

WORKDIR /build/

RUN apk add --update alpine-sdk postgresql-dev

ARG PGEXTWLIST_VERSION="1.17"
RUN curl -sSfL https://github.com/dimitri/pgextwlist/archive/refs/tags/v${PGEXTWLIST_VERSION}.tar.gz \
       -o pgextwlist.tar.gz \
 && echo "183eae8b80e61431f0950b29a2552dd20ceebc9cb63f8fe40e48c84d6e6c033b  pgextwlist.tar.gz" | sha256sum -c - \
 && tar xvzf pgextwlist.tar.gz \
 && cd pgextwlist-${PGEXTWLIST_VERSION} \
 && make \
 && make install \
 && cp pgextwlist.so .. \
 && cd - \
 && rm pgextwlist.tar.gz pgextwlist-${PGEXTWLIST_VERSION} -rf

FROM ${BASE_IMAGE}:${BASE_IMAGE_VERSION}

COPY --from=ext_builder /build/pgextwlist.so pgextwlist.so

RUN mkdir "$(pg_config --pkglibdir)/plugins" \
 && install pgextwlist.so "$(pg_config --pkglibdir)/plugins/pgextwlist.so"

COPY postgresql.conf /var/lib/postgresql/

# Add the initialisation scripts
COPY entry/* /docker-entrypoint-initdb.d/

# Force the modified postgres configuration ito use
CMD ["-c", "config_file=/var/lib/postgresql/postgresql.conf"]
