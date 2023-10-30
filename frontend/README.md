<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# eVaka frontend

This project contains all the frontend applications for eVaka. eVaka is an ERP for early childhood education.

The user interface is split into three applications, by user functionality:

- `employee-frontend` – Frontend application for the employee of the city. Depending on given roles, employee may handle
  e.g. early childhood education application process or operational tasks at the daycare.
- `employee-mobile-frontend` – Mobile frontend application for the employee of the city.
- `enduser-frontend` – Frontend application for the citizen. Citizens may e.g. apply to early childhood education, or
  browse decisions made for their dependants.

There are also some internal subprojects:

- `lib-common` - Common code usable in all frontends and E2E tests
- `lib-components` - Common React components usable in all frontends
- `lib-customizations` - Customizable code and assets for eVaka forks, e.g. localizations
- `lib-icons` - Icon set, switchable between free and pro Font Awesome
- `maintenance-page-frontend` - Static website to be shown during maintenance instead of the other frontends
- `e2e-test` - E2E tests

**NOTE:** This project is currently under very active development.
At this point, it is not yet adviced to start developing or running your custom
solution based on this project. Most likely, the project will evolve
considerably before reaching more stable phase. Please refer to
project's roadmap for further information regarding development plans.
That said, feel free to explore the project and try running it locally.

## Requirements

Following dependencies are required for running eVaka frontend
applications locally. You can install them through your development
environment's package manager, or alternatively download binaries
from websites provided below.

- [Node.js](https://nodejs.org/en/) – a JavaScript runtime built on
  Chrome's V8 JavaScript engine, version 20.9+
  - Install correct version automatically with [nvm](https://github.com/nvm-sh/nvm): `nvm install` (see [`.nvmrc`](../.nvmrc))
- [Yarn](https://yarnpkg.com/getting-started/install) – Package manager for Node, version 1.22+. The globally installed
  yarn is only used as a launcher, and a specific yarn version included in the eVaka repository is automatically used
  for all actual work.

## Overview of the repository structure

The code is organised into `src/` directory as follows:

```txt
src/
  |- *-frontend/
  |- lib-*/
  '- e2e-test
```

## Font Awesome icon library

The project uses [Font Awesome](https://fontawesome.com/) library as
an icon library. **You can run the application using the free version
of Font Awesome**. Alternatively, you could purchase the license for
professional version of Font Awesome, which will give the application
a better look and feel with regards to icons.

### Using free icons

All builds will use the free icons if the pro icons have not been installed,
but you can also explicitly set the environment variable `ICONS=free`, e.g.:

```sh
ICONS=free yarn dev
```

### Using professional icons

Please refer to [Font Awesome documentation](https://fontawesome.com/plans)
on how to obtain a commercial license for the professional version of the icon library.

Installed pro icons are used automatically, but you can also explicitly set the
environment variable `ICONS=pro` when running any builds, e.g.:

```sh
ICONS=pro yarn dev
```

To fetch the icons, see:

- [Instructions for developers with a license for Font Awesome Professional icons](#instructions-for-developers-with-a-license-for-font-awesome-professional-icons)
- [Instructions for Voltti developers](#instructions-for-voltti-developers)

#### Instructions for developers with a license for Font Awesome Professional icons

Once you have a license, configure **temporary** access to Font Awesome's
private registry in your `.npmrc`:

```ini
@fortawesome:registry=https://npm.fontawesome.com/
//npm.fontawesome.com/:_authToken=<your token>
```

e.g. Voltti developers can configure it with:

```sh
cat << EOF > .npmrc
@fortawesome:registry=https://npm.fontawesome.com/
//npm.fontawesome.com/:_authToken=$(aws ssm get-parameter --name "/voltti/fontawesome-token" --query 'Parameter.Value' --with-decryption --region eu-west-1 --profile voltti-dev)
EOF
```

**IMPORTANT:** Once you have fetched the packages, remove the above configuration
to avoid accidentally configuring all the public `@fortawesome/` packages to
target the private registry in `yarn.lock`.

Fetch and place the packages with:

```sh
./setup-pro-icons.sh
```

## Development

Before beginning, please remember to install all required dependencies
using `yarn install`.

### Linting

All frontends are currently in the process of being migrated to ESLint from TSLint. Please refer to each package's own
configuration (in `package.json` files) for more details about the linting rules. We also have some
[custom ESLint rules](#custom-eslint-rules)

```bash
# Lint all files
# Add --fix to fix lint errors automatically
yarn lint
```

#### Custom ESLint rules

For rules not covered by existing ESLint rulesets, we can create our own in [`./eslint-plugin/`](./eslint-plugin/).

Some official documentation:

- [Working with Rules](https://eslint.org/docs/developer-guide/working-with-rules)
- [Selectors](https://eslint.org/docs/developer-guide/selectors)

### Unit tests

Unit tests are written using [Jest](https://jestjs.io/)
JavaScript testing framework.

```bash
# Run all tests
yarn test
```

### E2E tests

E2E tests are written using Typescript, Jest and Playwright.

**NOTE:** For all test scripts, you can override the target URL with:

```sh
BASE_URL=http://localhost:9099 yarn <cmd>
```

#### Creating a new test

- Create a new test under the right test folder (= enduser)
- Name test file clearly to represent test case
- Oraganize tests into categories ([describe](https://jestjs.io/docs/api#describename-fn))
- Create [Page objects](https://playwright.dev/docs/pom) for pages/views that are tested

#### Run all tests locally

All E2E tests can be run with the following command:

```sh
yarn e2e-test
```

If you don't want to wipe data from your dev environment,
then start the dev server by running the following commands in
the `service` directory before running the playwright tests:

```sh
pm2 stop service
./gradlew bootRunTest
```

You can run a single spec by specifying the corresponding subdirectory. For example,
in order to only run tests related to messaging, use the following command:

```sh
yarn jest --runInBand src/e2e-test/specs/7_messaging
```

[Playwright traces](https://playwright.dev/docs/trace-viewer) are collected from
failed tests to the `traces/` directory. Use the following command to inspect a
trace in the Playwright trace viewer:

```
yarn exec playwright show-trace traces/<filename>.zip
```

#### Scheduled CircleCI run for wip branch

```yaml
e2e_test_wip:
  triggers:
    - schedule:
        # Test servers are up 6-20. Using UTC time, taking into account daylight saving time
        # your own timestamps here
        cron: '0,10,20,30,40,50 4-18 * * 1-5'
        filters:
          branches:
            only:
              - EVAKA-111-your-branch-name-here
  jobs:
    - lint
    - e2e-smoke:
        requires:
          - lint
    - e2e-regression:
        requires:
          - e2e-smoke
```

[The cron schedule expression editor](https://crontab.guru/)

#### Known issues

- Some selectors still use IDs/classes instead of data-qa attributes
- Should replace for loops by selector's visibilitycheck option
- Tests aren't run against multiple different browsers

### Tips & tricks

#### Overriding backend URLs

When running any local development servers, set the `API_PROXY_URL` to
override the default API proxy URL.
This is useful if you want to use the [`compose-e2e` stack](compose/README.md) for running the backend services.

## Deployment

### Configuration and feature flags

All configuration of frontend apps is done as [customizations](#customizing-the-frontend)
in [`lib-customizations`](./src/lib-customizations).

Configuration is split into two categories:

1. App configuration: global app (and optionally environment) specific configs
1. Feature flags: generally global toggles (and optionally environment and/or
   app specific toggles) to enable eVaka features

and feature flags are further split into:

1. Optional features that are considered production ready
1. Optional in-development (`experimental.*`) features that can be enabled
   in eVaka customizations to test in e.g. non-production environments

Customizations make no assumptions and provide no direct tooling for ways to
make environment specific configurations (though examples may be provided
in the included `espoo` theme) but instead this is left to the customizers.

For more details, [see below](#customizing-the-frontend).

### Production builds

Production build compiles the application into a minimized and optimized
version, that can then be served for the users through static file
hosting.

To create a production build, run

```bash
yarn build
```

**NOTE:** CI builds a **single build for all environments** (per frontend).
Therefore, all environment-specific configs/feature flags should be done
as customizations and only build-time configuration should be placed in `.env*`
files.

This allows us to keep the CI loop shorter by avoiding duplicate
builds, and also to use complex types in configs (instead of strings).

### Error logging and monitoring

eVaka has a support for using [Sentry](https://sentry.io) for error
logging in the frontend applications. Sentry's application monitoring
platform is a commercial product, that is not free to use.
**Please note, that you can run the application also without Sentry.**

## Customizing the frontend

See the [docs in `src/lib-customizations/`](./src/lib-customizations/README.md).
