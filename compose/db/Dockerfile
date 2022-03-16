# SPDX-FileCopyrightText: 2017-2021 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

ARG BASE_IMAGE=library/postgres
ARG BASE_IMAGE_VERSION=14-alpine


FROM ${BASE_IMAGE}:${BASE_IMAGE_VERSION} AS ext_builder
# Extension whitelisting to simulate the RDS behaviour
# Build https://github.com/dimitri/pgextwlist package from source, transfer it to target installation
RUN apk add --update alpine-sdk postgresql-dev
RUN git clone https://github.com/dimitri/pgextwlist.git /tmp/pgextwlist
WORKDIR /tmp/pgextwlist
RUN make && make install


FROM ${BASE_IMAGE}:${BASE_IMAGE_VERSION}

COPY --from=ext_builder /tmp/pgextwlist/pgextwlist.so /tmp/pgextwlist/pgextwlist.so
RUN mkdir "$(pg_config --pkglibdir)/plugins" \
 && install /tmp/pgextwlist/pgextwlist.so "$(pg_config --pkglibdir)/plugins/pgextwlist.so"
COPY postgresql.conf /var/lib/postgresql/

# Add the initialisation scripts
COPY entry/* /docker-entrypoint-initdb.d/

# Force the modified postgres configuration ito use
CMD ["-c", "config_file=/var/lib/postgresql/postgresql.conf"]
