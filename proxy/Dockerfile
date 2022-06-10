# SPDX-FileCopyrightText: 2017-2021 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

ARG NGINX_VERSION=1.21.6
FROM nginx:${NGINX_VERSION}

LABEL maintainer="https://github.com/espoon-voltti/evaka"

ENV NGINX_ENV=local \
    TZ=UTC

ARG CACHE_BUST=2022-06-10

RUN apt-get update \
 && apt-get -y dist-upgrade \
 && apt-get -y --no-install-recommends install ruby \
 && curl -sSfL https://github.com/espoon-voltti/s3-downloader/releases/download/v1.3.0/s3downloader-linux-amd64 \
       -o /bin/s3download \
 && chmod +x /bin/s3download \
 && echo "d0ee074cbc04c1a36fb8cee6f99d9ff591fee89ea38d34a328d0ee1acb039a48  /bin/s3download" | sha256sum -c - \
 && rm -rf /var/lib/apt/lists/*


# https://docs.datadoghq.com/tracing/setup_overview/proxy_setup/?tab=nginx # update version using get_latest_release
ARG OPENTRACING_NGINX_VERSION=v0.24.0
ARG OPENTRACING_NGINX_SHA256="0873da40f53566273336e8ec941bd713377e1c4af07bed2ca30280c18fb4aba1"
ARG DD_OPENTRACING_CPP_VERSION=v1.3.1
ARG DD_OPENTRACING_CPP_SHA256="fee2a0340e340b8042a245ac6421175c755e23e08e3f109576038a2322305082"

RUN cd /tmp \
 && curl -sSfLO "https://github.com/opentracing-contrib/nginx-opentracing/releases/download/${OPENTRACING_NGINX_VERSION}/linux-amd64-nginx-${NGINX_VERSION}-ot16-ngx_http_module.so.tgz" \
 && echo "${OPENTRACING_NGINX_SHA256}  linux-amd64-nginx-${NGINX_VERSION}-ot16-ngx_http_module.so.tgz" | sha256sum -c - \
 && tar zxf "linux-amd64-nginx-${NGINX_VERSION}-ot16-ngx_http_module.so.tgz" -C /usr/lib/nginx/modules \
 && rm "linux-amd64-nginx-${NGINX_VERSION}-ot16-ngx_http_module.so.tgz" \
 && curl -sSfLO https://github.com/DataDog/dd-opentracing-cpp/releases/download/${DD_OPENTRACING_CPP_VERSION}/linux-amd64-libdd_opentracing_plugin.so.gz \
 && echo "${DD_OPENTRACING_CPP_SHA256}  linux-amd64-libdd_opentracing_plugin.so.gz" | sha256sum -c - \
 && gunzip linux-amd64-libdd_opentracing_plugin.so.gz -c > /usr/local/lib/libdd_opentracing_plugin.so \
 && rm linux-amd64-libdd_opentracing_plugin.so.gz

COPY ./files/ /
ENTRYPOINT ["/bin/proxy-entrypoint.sh"]
CMD ["nginx", "-g", "daemon off;"]

# Add build and commit environment variables and labels
# for tracing the image to the commit and build from which the image has been built.
ARG build=none
ARG commit=none
ENV APP_BUILD="$build" \
    APP_COMMIT="$commit"
LABEL fi.espoo.build="$build" \
      fi.espoo.commit="$commit"
