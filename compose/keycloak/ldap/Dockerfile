# SPDX-FileCopyrightText: 2017-2020 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

from debian:bullseye

RUN apt-get -y update \
 && apt-get -y dist-upgrade \
 && apt-get -y install ldap-utils

ADD ldap.conf /etc/ldap/ldap.conf
