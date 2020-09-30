<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# eVaka Employee Frontend

## Packages

The project uses a commercial version of [Font Awesome](https://fontawesome.com/), so you need to [configure that first](../README.md#access-to-professional-icons).

If you want to use the free icons instead, set an environment variable `ICONS=free`.

## Development

Start a dev server that proxies api requests to locally running [internal-gw](../../../apigw/README.md)

```sh
yarn dev
```

Run linter with optional autofix

```sh
yarn lint[:fix]
```

Run unit tests

```sh
yarn test
```
