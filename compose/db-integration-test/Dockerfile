# SPDX-FileCopyrightText: 2017-2020 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

FROM postgres:10.13-alpine

ENV POSTGRES_USER=postgres POSTGRES_PASSWORD=postgres
COPY *.sql /docker-entrypoint-initdb.d/
CMD ["postgres", "-c", "fsync=off"]
