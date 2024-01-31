<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# evaka-api-gateways

Gateway between eVaka frontends and backend services

- enduser gateway
- internal gateway

## Requirements

- [Node.js](https://nodejs.org/en/) – a JavaScript runtime built on Chrome's V8 JavaScript engine, version 20.9+
  - Install correct version automatically with [nvm](https://github.com/nvm-sh/nvm): `nvm install` (see [`.nvmrc`](../.nvmrc))
- [Yarn](https://yarnpkg.com/getting-started/install) – Package manager for Node, version 1.22+

The service requires redis running on port 6379. Easiest way is to run it with [compose](../compose/README.md) command

```bash
docker compose up -d redis
```

## Development

Install dependencies

```bash
yarn
```

Start development server with hot reloading. Requires running backend services.

```bash
yarn dev
```

Run tests

```bash
yarn test
```

Lint with auto-fix

```bash
yarn lint
yarn lint:fix
```

## Run with Docker

To run with Docker Compose, check the instructions in [compose README](../compose/README.md).
You can build a local image by running

```bash
./build-docker.sh
```

## Keys

Each gateway needs a set of keys to communicate with evaka services. New keys can be generated as follows:

```bash
openssl genpkey -out jwk_private_key.pem -algorithm RSA -pkeyopt rsa_keygen_bits:4096
openssl rsa -in jwk_private_key.pem -pubout > jwk_public_key.pem
```

The public key can be converted to jwk-format with

```bash
npx pem-jwk jwk_public_key.pem > public.jwk
```

You then need to add `"kid": <service name>` pair to the `public.jwk` contents, eg.

```json
{
    "kid": "evaka-internal-gw",
    "kty": "RSA",
    "n": ...,
    "e": "AQAB"
}
```

The pem files must be put into a **private** S3 buckets accessible to the services, into gateway-specific directories
like: `s3://my-deployment-bucket-<env>/<gateway>/`. The contents of `public.jwk` must be copied into each related
service's `keys` list in `jwks.json`, which should be similarly found in `s3://my-deployment-bucket-<env>/<service>/`.

You can put the generated files to `dev` environment's bucket with e.g.:

```bash
# For Voltti environments, you can find the bucket ID from Terraform output
export DEPLOYMENT_BUCKET=<REPLACE ME>
echo aws s3 cp jwks.json "s3://${DEPLOYMENT_BUCKET}/evaka-srv/jwks.json"
for VER in public private; do
    echo aws s3 cp "jwk_${VER}_key.pem" "s3://${DEPLOYMENT_BUCKET}/internal-gw/jwk_${VER}_key.pem"
done
```

Same goes for `test`, `staging` and `prod` but *be sure to use different keys* in all environments.

## Authentication and session

Session data is store in ElastiCache for Redis. Session includes the logged in user details required by passport
and SAML to log out the user with Single Sign Out. The session also stores the user UUID. Downstream calls are
authenticated with JWK-signed JWT:s that contain the user ID. To add an authentication header to a downstream call
use the `createAuthHeaders(user)` function from `shared/auth/index.ts`. The user parameter is the user object
from a logged in session (available at `req.user`).

## Security

### Headers

The project uses Helmet to set sane default headers and remove unsafe headers set by Express. Many of these headers
are already set at the proxy, but helmet was chosen to be kept with a default setting to incorporate possible additions
that don't get updated to the proxy.

### XSRF Protection

After login an `XSRF-TOKEN` cookie is set with a random token value. The csurf middleware check all incoming traffic
with HTTP POST methods for headers containing the token. Axios HTTP client automatically recognizes this cookie and
send a `X-XSRF-TOKEN` header with the cookie value.

Since the domain of the service is shared among multiple services all with different sessions and XSRF cookies. Some
requests might fail when browsing multiple application simultaneously.

### Session handling

The session identifier of the login cookie is rolled after login to mitigate session fixation attacks.

The session cookie is set without the secure flag, since the connection to the proxy is not secured and Express refuses
to set the cookie. The cookie flag is ultimately set at the proxy.
