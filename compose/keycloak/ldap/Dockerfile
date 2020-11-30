from debian:bullseye

RUN apt-get -y update \
 && apt-get -y dist-upgrade \
 && apt-get -y install ldap-utils

ADD ldap.conf /etc/ldap/ldap.conf
