#!/bin/sh -e

# SPDX-FileCopyrightText: 2017-2020 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

FILES_ROOT=$1
find "$FILES_ROOT" -name 'V*sql' -exec basename {} \; | sort -n -t '.' -k 1.2 -k 2 -k 3
