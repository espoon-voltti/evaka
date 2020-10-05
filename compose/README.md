<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# eVaka compose

This repository contains everything needed to run eVaka projects
on local development locally.

## Prerequisites

These are the prerequisites to run environment locally.

### Required tooling

eVaka uses [PM2](https://pm2.keymetrics.io/) for running multiple
services in parallel. PM2 is an opinionated choice for
development process in eVaka. However, installing PM2 is not strictly
mandatory. Alternatively, you could run every sub-project separaterly.

Install PM2 by running following command

```bash
npm install -g pm2
```

### Dependencies

Following dependencies are required for running eVaka development
environment locally. You can install them through your development
environment's package manager, or alternatively download binaries
from websites provided below.

- [Node.js](https://nodejs.org/en/) – a JavaScript runtime built on Chrome's V8 JavaScript engine, version 10
- [Yarn](https://yarnpkg.com/getting-started/install) – Package manager for Node
- [JDK](https://openjdk.java.net/projects/jdk/11/) – Java Development
  Kit, version 11+. We recommend using OpenJDK implementation of JSR 384.
- [Docker](https://docs.docker.com/get-docker/) – Docker is an open platform for developing, shipping, and running applications.

## Starting all sub-projects in development mode

A configuration file for PM2 is provided in this repository. Thus,
you can start all projects in development mode easily.

**Please note** The projects need to be first set up correctly
for this to work. For example, `.npmrc` files for private dependencies
need to be in place, Spring Boot services need the local deployment
files, etc…).

First, start the third-party dependencies using `docker-compose`:

```bash
docker-compose up -d
```

Then, start eVaka projects with PM2:

```bash
pm2 start
```

`yarn install` might fail on frontends when running `pm2 start`. To
fix this, try to running `yarn cache clean && yarn install` in failing
repositories.

### Useful commands

```bash
pm2 status # Shows status
pm2 stop all # Stops all processes
pm2 stop apigw # Stops one process (apigw)
pm2 restart application-srv # Restarts one process (application-srv)
pm2 logs apigw # Shows logs from one process (apigw)
pm2 delete all # Deletes all configured processes. Use this if ecosystem.config.js has changed
pm2 flush # Clears old logs
```

## Accessing the application

Once the development environment is correctly set up, you can access
the frontends at following URLs:

- <http://localhost:9091> – Frontend for the citizen
- <http://localhost:9093/employee> – Frontend for the employee roles

## Running the full stack for E2E tests

Running the application with `./compose-e2e` is mainly
designed for running E2E tests locally and in continuous integration
(CI) pipelines.

First, you need to build frontends and all the docker images.

With [free icons](../frontend/README.md#using-free-icons)

```sh
./build.sh
```

With [pro icons](../frontend/README.md#using-pro-icons)

```sh
ICONS=pro ./build.sh
```

Then, start the whole stack locally. `compose-e2e` is just a
wrapper for `docker-compose`.

```sh
./compose-e2e up -d
```

Access the frontends at

- <http://localhost:9999/> – Frontend for the citizen
- <http://localhost:9999/employee> – Frontend for the employee

## Troubleshooting

### Database

Sometimes, the shared database can get into an inconsistent state and
the stack won't start properly. Or, the applications are unable to use
it properly. This might be caused by the way the database stores
its state locally between different runs.

One symptom of this is a service logging the message "Hint: Must be
superuser to create this extension".

This can be fixed by recreating the database docker image and resetting the database docker volume.

The service specific database setup scripts are located in the
service repositories. The database image using them is built with
`build.sh` script. If a service specific database setup script is
changed, then `build.sh` must be run to get the change as a part of the
build.

```bash
# To clear the database simply zap the volumes with:
docker volume rm <volume_name>

# where <volume_name> can be found either with
docker volume ls

# or inspecting the container bindings
docker inspect <container> | jq '.[] | .HostConfig.Binds[]'

# or simply remove all volumes with
docker volume prune

# or take down services and volumes altogether with
docker-compose down -v
```
