<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# eVaka compose

This repository contains everything needed to run eVaka projects
on local development environment.

## Prerequisites

These are the prerequisites to run environment locally.

### Development environments and operating systems

eVaka is being actively developed in Linux and macOS environments.
We recommend using the package manager (e.g. `aptitude`, `homebrew` etc.)
for your operating system for obtaining required software and packages.

Development in Windows environment should also be possible, but we
cannot guarantee it works out-of-the-box. In any case, we strongly recommend
installing [Windows Subsystem for Linux](https://docs.microsoft.com/en-us/windows/wsl/install-win10)
for developing in Windows operating system.

### Dependencies

Following dependencies are required for running eVaka development
environment locally. You can install them through your development
environment's package manager, or alternatively download binaries
from websites provided below.

Please note that all the dependencies should be installed as
current user. Do not use elevated privileges, e.g. sudo to install
packages.

- [Node.js](https://nodejs.org/en/) – a JavaScript runtime built on Chrome's V8 JavaScript engine, version 18.17+
  - Install correct version automatically with [nvm](https://github.com/nvm-sh/nvm): `nvm install` (see [`.nvmrc`](../.nvmrc))
- [Yarn](https://yarnpkg.com/getting-started/install) – Package manager for Node, version 1.22+
- [JDK](https://openjdk.java.net/projects/jdk/21/) – Java Development
  Kit, version 21. We recommend using OpenJDK.
- [Docker](https://docs.docker.com/get-docker/) – Docker is an open platform for developing, shipping, and running applications.
- [docker compose](https://docs.docker.com/compose/install/) - Tool for running multi-container Docker applications, version 1.26.0+

### Required tooling

eVaka uses [PM2](https://pm2.keymetrics.io/) for running multiple
services in parallel. PM2 is an opinionated choice for
development process in eVaka. Installing PM2 is not strictly
mandatory. You can also run every sub-project separately or
build Docker images and [run the stack using Docker](#running-the-full-stack-for-e2e-tests)).

Install PM2 by running following command

```bash
npm install -g pm2
```

You will also need the `nc/netcat` (Arbitrary TCP and UDP connections and listens) utility.
If your operating system does not have this utility installed, please install
it using your package manager. (E.g. on Ubuntu, run `sudo apt-get install netcat`).

## Starting all sub-projects in development mode

A configuration file for PM2 is provided in this repository. Thus,
you can start all projects in development mode easily.

*Please note** The sub-projects may need additional configuration before
local development can **fully** be done. For example, `.npmrc` files for
private dependencies like professional icons used in frontend need to be
in place, Spring Boot services need the local deployment files, etc…).
See sub-projects' README files for more information. At the time of writing,
you can skip this, if you just want to see if you can get everything running
locally.

First, start the third-party dependencies using `docker compose`:

```bash
docker compose up -d
```

Then, start eVaka projects with PM2:

```bash
pm2 start
```

### Useful commands

```bash
pm2 status # Shows status
pm2 stop all # Stops all processes
pm2 stop apigw # Stops one process (apigw)
pm2 restart service # Restarts one process (service)
pm2 logs apigw # Shows logs from one process (apigw)
pm2 delete all # Deletes all configured processes. Use this if ecosystem.config.js has changed
pm2 flush # Clears old logs
```

## Accessing the application

Once the development environment is correctly set up, you can access
the frontends at following URLs:

- <http://localhost:9099> – Frontend for the citizen
- <http://localhost:9099/employee> – Frontend for the employee roles
- <http://localhost:9099/employee/mobile> – Frontend for the employee mobile frontend

## Running the full stack for E2E tests

Running the application with `./compose-e2e` is designed
for running E2E tests locally and in continuous integration
(CI) pipelines.

Start the whole stack locally:

```sh
./compose-e2e up -d
```

`compose-e2e` is simple wrapper for `docker compose`.

Access the frontends at

- <http://localhost:9099/> – Frontend for the citizen
- <http://localhost:9099/employee> – Frontend for the employee
- <http://localhost:9099/employee/mobile> – Frontend for the employee mobile

## Running tests inside docker compose

To run tests inside `docker compose` locally.

```sh
./test-e2e build
./test-e2e run playwright
```

### Updating Playwright

To update Playwright, modify the version in `frontend/package.json` and generate `frontend/yarn.lock`. The updated version will be automatically used in E2E tests.

The E2E resources will be installed on every run and it always uses what is specified in `yarn.lock`. The dependencies will be cached in the new E2E image when changes are merged into the master-branch and CI is completed.

#### Updating E2E Image

This applies only when changing image files in `compose/e2e/*`. When updating only Playwright this is not required.

Test chages to E2E-image locally:

```sh
./test-e2e build playwright # Build compose/e2e/playwright.Dockerfile
./test-e2e run playwright # Run E2E tests using build image
```

If changes need to be tested in CI before merging:

1. Push branch changes.
2. Tag the branch head commit.
```sh
git tag -f test-playwright
git push --force origin tag test-playwright
```
3. Wait for successful completion of e2e:playwright job.
4. Invoke the GitHub Actions Workflow.
    - Select branch.
    - Set `playwright_tag` as `test-playwright`.
    - Run the workflow.
5. Check CI for results.
6. Remove the tag when finished.
```sh
git tag --delete playwright-test
git push --delete origin playwright-test
```

## Database dump

To dump local database run `./db.sh dump` and restore it with `./db.sh restore`.
Optional dump name can be given to script, example `./db.sh dump my.dump`.

## Keycloak

Keycloak admin login from <http://localhost:8080/auth/admin/master/console/> with credentials `admin:admin`.

Evaka-customer realm (<http://localhost:9099/api/application/auth/evaka-customer/login?RelayState=%2F>) has pre-configured user with credentials `johannes.karhula@evaka.test:test123`.

## Troubleshooting

### Database

**Please note** Ensure you do not have another PostgreSQL server listening
on the default port. If you do, eVaka service might fail to start trying to
connect to a wrong database causing errors like
`FATAL: role "evaka_migration_local" does not exist` appear on service log.

Sometimes, the shared database can get into an inconsistent state and
the stack won't start properly. Or, the applications are unable to use
it properly. This might be caused by the way the database stores
its state locally between different runs.

One symptom of this is a service logging the message "Hint: Must be
superuser to create this extension".

This can be fixed by recreating the database docker image and resetting the database docker volume.

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
docker compose down -v

docker compose build db
```

### Unable to start services correctly

In case you are unable to start services properly (i.e. `pm2 status` command will)
list some of the services `stopped`), please first double-check all the instructions
in this document.

Then, for investigating issues with specific service, please refer to the application logs.
You can list the application logs for e.g. `service` or `apigw` with following commands:

```bash
# List logs for service
pm2 logs service
# List logs for apigw
pm2 logs apigw
```

You should be able to start all the services without any modifications
if you have followed the instructions carefully.
