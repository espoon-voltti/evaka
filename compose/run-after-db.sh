#!/bin/bash

# SPDX-FileCopyrightText: 2017-2020 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

set -euo pipefail

until nc -z "localhost" "5432"; do
  echo "Waiting for PostgreSQL"
  sleep 1
done

exec $*
