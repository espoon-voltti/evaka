<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# evaka-proxy

Simple nginx proxy image for eVaka frontends and API calls via frontends.

**NOTE:** As this image just a proxy for S3 and ECS services,
the recommended way to test it is to build an image locally and deploy it
to the dev environment (meant for infra testing).

## Configuration

This image uses ERB (i.e. Ruby) templating for the `nginx.conf.template`.

Currently available configuration options (via environment variables):

| Name | Description | Required | Default |
|------|-------------|----------|:-----:|
| `NGINX_ENV` | Deployment environment name, for logging | Y | `'local'` |
| `STATIC_FILES_ENDPOINT_URL` | URL for static frontend files to be proxied | Y | `''` |
| `ENDUSER_GW_URL` | Enduser API Gateway's URL | Y | `''` |
| `INTERNAL_GW_URL` | Internal (employee) API Gateway's URL | Y | `''` |
| `BASIC_AUTH_ENABLED` | Is basic authentication enabled. If yes, requires `BASIC_AUTH_CREDENTIALS` to be set. | N | `false` |
| `BASIC_AUTH_CREDENTIALS` | Basic authentication credentials in `.htpasswd` format, like `user:hashofpassword`. Requires `BASIC_AUTH_ENABLED` to be `true` to be active. | N | `''` |
| `RATE_LIMIT_CIDR_WHITELIST` | Semi-colon delimited string of whitelisted CIDR(s) | N | `''` |
| `SECURITYTXT_CONTACTS` | Semi-colon delimited string of Contact values for `security.txt`. Remember to include "https://" for URLs, and "mailto:" for e-mails. Non-empty values enable the `security.txt` route (`/.well-known/security.txt`) | N | `''` |
| `SECURITYTXT_LANGUAGES` | Preferred-Languages value for `security.txt`. Requires `SECURITYTXT_CONTACTS` to be non-empty to be active. | N | `'en'` |

For `security.txt` configuration, see [the latest Internet draft](https://tools.ietf.org/html/draft-foudil-securitytxt-10)

## Development

Build image locally:

```sh
./build.sh

# Optionally override tag name
DOCKER_TAG=myuniquetag ./build.sh
```

### Testing configuration

Nginx configuration is smoke tested automatically in the first stage of the
Docker image's multi-stage build, so you can test the configuration's validity:

```sh
./build.sh
```

**NOTE:** All available configuration options should be set for the tests
