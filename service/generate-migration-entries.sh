#!/bin/sh -e

# SPDX-FileCopyrightText: 2017-2020 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

FILES_ROOT=$1
find "$FILES_ROOT" -maxdepth 1 -name 'V*sql' | sed "s|^$FILES_ROOT/||" | sort -n -t '.' -k 1.2 -k 2 -k 3
