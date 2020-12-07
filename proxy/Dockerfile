# SPDX-FileCopyrightText: 2017-2020 City of Espoo
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

RUN apk add --no-cache ruby=2.7.1-r3

COPY proxy_params /etc/nginx/proxy_params
COPY nginx.conf.template /etc/nginx/conf.d/evaka-nginx.conf.template

RUN erb /etc/nginx/conf.d/evaka-nginx.conf.template > /etc/nginx/conf.d/evaka-nginx.conf \
      && echo "$BASIC_AUTH_CREDENTIALS" > /etc/nginx/.htpasswd
RUN nginx -c /etc/nginx/nginx.conf -t && cat /etc/nginx/conf.d/evaka-nginx.conf

FROM nginx:1.19-alpine

LABEL maintainer="https://github.com/espoon-voltti/evaka"

ENV NGINX_ENV local
ENV TZ UTC

RUN apk add --no-cache ruby=2.7.1-r3

COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh
COPY proxy_params /etc/nginx/proxy_params
COPY nginx.conf.template /etc/nginx/conf.d/evaka-nginx.conf.template

# Add build and commit environment variables and labels
# for tracing the image to the commit and build from which the image has been built.
ARG build=none
ARG commit=none
ENV APP_BUILD "$build"
ENV APP_COMMIT "$commit"
LABEL fi.espoo.build="$build" \
      fi.espoo.commit="$commit"

CMD ["/entrypoint.sh"]
