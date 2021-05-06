# SPDX-FileCopyrightText: 2017-2021 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

# Run a simple config test with all the supported configs set.
# Intended to catch configuration issues in CI.
FROM nginx:1.19-alpine as smoketest

ENV BASIC_AUTH_CREDENTIALS 'smoketest:$apr1$m0p2wy4c$OcpUTIZ4za1mRVxt6DuEs/'
ENV BASIC_AUTH_ENABLED true
ENV ENDUSER_GW_URL http://fake.test
ENV HOST_IP smoketest
ENV INTERNAL_GW_URL http://fake.test
ENV KEYCLOAK_URL http://fake.test
ENV NGINX_ENV smoketest
ENV RATE_LIMIT_CIDR_WHITELIST '10.0.0.0/8;192.168.0.0/16'
ENV SECURITYTXT_CONTACTS 'mailto:fake@fake.test;mailto:another@fake.test'
ENV SECURITYTXT_LANGUAGES fi,se,en
ENV STATIC_FILES_ENDPOINT_URL http://fake.test

RUN apk add --no-cache 'ruby=~2.7'

COPY ./files/ /

RUN erb /etc/nginx/conf.d/nginx.conf.template > /etc/nginx/conf.d/nginx.conf \
 && echo "$BASIC_AUTH_CREDENTIALS" > /etc/nginx/.htpasswd \
 && nginx -c /etc/nginx/nginx.conf -t && cat /etc/nginx/conf.d/nginx.conf
