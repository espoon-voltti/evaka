<!--
SPDX-FileCopyrightText: 2017-2022 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# Evaka Keycloak

This project has Evaka [KeyCloak](https://www.keycloak.org/). Theme is based to [Helsinki KeyCloak Theme](https://github.com/City-of-Helsinki/helsinki-keycloak-theme).

## docker compose

#### Usage with Evaka docker compose

Evaka docker compose already has KeyCloak. To develop KeyCloak theme replace it with compose found here.

```bash
cd ../compose
docker compose up -d

docker compose stop keycloak smtp

cd ../keycloak
./compose-keycloak up
```

### Local testing

KeyCloak admin is at <http://localhost:8080/auth/admin> with credentials `admin:admin`

You can test the login and signup flows in the following URLs:

- <http://localhost:8080/auth/realms/evaka/account/> (`evaka` realm, for palveluntuottaja)
- <http://localhost:8080/auth/realms/evaka-customer/account/> (`evaka-customer` realm, for citizens)

Received emails can be accessed at <http://localhost:8025/>, but it must be configured for realms.

## Creating local user

[Create KeyCloak user using admin panel for selected realm](https://www.keycloak.org/docs/latest/server_admin/#assembly-managing-users_server_administration_guide).

To link KeyCloak user with Evaka set `suomi_nationalIdentificationNumber` for KeyCloak user.

### Configure

Mock KeyCloak Evaka realm is imported from `compose-resources/configuration/evaka.json`.

## Update

Update KeyCloak by changing version in `Dockerfile` and `pom.xml` files.
