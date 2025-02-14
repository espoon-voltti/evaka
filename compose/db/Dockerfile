# SPDX-FileCopyrightText: 2017-2023 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

FROM library/postgres:17-alpine3.21

COPY postgresql.conf /var/lib/postgresql/

# Add the initialisation scripts
COPY entry/* /docker-entrypoint-initdb.d/

# Force the modified postgres configuration into use
CMD ["-c", "config_file=/var/lib/postgresql/postgresql.conf"]
