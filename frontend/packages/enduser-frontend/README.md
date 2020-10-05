<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# enduser-frontend

eVaka frontend for municipal applicants.

Made with [Vue](https://vuejs.org/).

## Requirements

* Node version 10.16
* Yarn version 1.16

## Prerequisites for running locally

These microservices can either be run with [compose](../../../compose/README.md) or locally with a terminal/IDE.

* The following microservices have to be running locally:
  * [service](../../../service/README.md)
  * [enduser-gw](../../../apigw/README.md)
  * evaka-db (through docker-compose)

## Packages

The project uses a commercial version of [Font Awesome](https://fontawesome.com/),
so you need to [configure that first](../README.md#using-pro-icons)
and **always** set `ICONS=pro` when running builds/local dev servers.

## Development

```sh
# installing dependencies
yarn

# run dev server (with hot reload) at http://localhost:9091/
yarn dev
```

## Unit tests

```sh
yarn test
```

## E2E tests

E2E tests are located in their own package, [found here](../../e2e-test/README.md).

E2E tests are run as part of this repository's CI workflow.

## Lint

```sh
# lints and fixes files
yarn lint
```

## Building application

```sh
# build for production with minification
yarn build
```

For detailed explanation on how things work, checkout the [guide](http://vuejs-templates.github.io/webpack/) and [docs for vue-loader](http://vuejs.github.io/vue-loader).
