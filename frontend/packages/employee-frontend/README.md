<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# eVaka Employee Frontend

## Packages

The project uses a commercial version of [Font Awesome](https://fontawesome.com/),
so you need to [configure that first](../../README.md#using-professional-icons)
and **always** set `ICONS=pro` when running builds/local dev servers.

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
