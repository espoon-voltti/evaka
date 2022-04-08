# SPDX-FileCopyrightText: 2017-2020 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

FROM postgres:14-alpine

ENV POSTGRES_USER=postgres POSTGRES_PASSWORD=postgres
COPY postgresql.conf /var/lib/postgresql/
COPY *.sql /docker-entrypoint-initdb.d/
CMD ["-c", "config_file=/var/lib/postgresql/postgresql.conf"]
