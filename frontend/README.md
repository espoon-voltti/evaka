<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# eVaka frontend

This project contains all the frontend applications for eVaka. eVaka is an ERP for early childhood education.

The user interface is split into two separate applications,
by user functionality:

- [`employee-frontend`](packages/employee-frontend/README.md) – Frontend
  application for the employee of the city. Depending on given roles,
  employee may handle e.g. early childhood education application
  process or operational tasks at the daycare.
- [`enduser-frontend`](packages/enduser-frontend/README.md) – Frontend
  application for the citizen. Citizens may e.g. apply to early childhood education, or browse decisions made for their dependants.
- [`maintenance-page`](packages/maintenance-page/README.md) - Static website to be shown during maintenance instead of the other frontends

And into a shared library:

- [`lib-common`](packages/lib-common/README.md)

This repository uses [Yarn workspaces](https://yarnpkg.com/lang/en/docs/workspaces/) to share
common code between packages / applications. See further instructions below.

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
  Chrome's V8 JavaScript engine, version 14.15+
  - Install correct version automatically with [nvm](https://github.com/nvm-sh/nvm): `nvm install` (see [`.nvmrc`](../.nvmrc))
- [Yarn](https://yarnpkg.com/getting-started/install) – Package manager
  for Node, version 1.22+

## Overview of the repository structure

The code is organised into `packages/` directory as follows:

```txt
packages/
  |- *-frontend/
  |- lib-common/
```

- `*-frontend` - eVaka frontend applications
- `lib-common` - code shared by all applications, i.e. not React or Vue components

## Font Awesome icon library

The project uses [Font Awesome](https://fontawesome.com/) library as
an icon library. **You can run the application using the free version
of Font Awesome**. Alternatively, you could purchase the license for
professional version of Font Awesome, which will give the application
a better look and feel with regards to icons.

### Using free icons

By default, all builds will use the free icons but you can also explicitly set
then environment variable `ICONS=free`, e.g.:

```sh
cd packages/employee-frontend
ICONS=free yarn dev
```

### Using professional icons

Please refer to [Font Awesome documentation](https://fontawesome.com/plans)
on how to obtain a commercial license for the professional version of the icon library.

By default, all builds will use the free icons, so you must set the environment
variable `ICONS=pro` when running any builds, e.g.:

```sh
cd packages/employee-frontend
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

Next, fetch the packages with:

```sh
npm pack @fortawesome/fontawesome-pro@5.14.0 --userconfig=<path to your .npmrc>
# repeat for all packages
```

Place the fetched `.tgz` packages in the `./vendor/fortawesome/` directory,
and run `./unpack-pro-icons.sh` to install the packages to the `node_modules` directory.

#### Instructions for Voltti developers

Voltti developers can use the helper script to fetch and unpack the icon packages from
a private S3 bucket: `./init-pro-icons.sh && ./unpack-pro-icons.sh`

For updating the icons, follow [the non-Voltti developer guide](#instructions-for-developers-with-a-license-for-font-awesome-professional-icons)
for fetching the new versions. Once fetched, upload the packages to S3:

```sh
aws --profile voltti-local s3 sync ./vendor/fortawesome/ s3://evaka-deployment-local/frontend/vendor/fortawesome/
```

## Development

The development process supports instant changes made to source code.
I.e. when running the applications in development mode, you should see
changes you make instantly in the browser.

Before beginning, please remember to install all required dependencies
using `yarn install`.

### Useful commands when using `yarn`

Here is a list of useful commands when using yarn workspaces.

```bash
# Install dependencies for all workspaces
yarn install

# Run a command on all workspaces
yarn workspaces run <cmd>
# e.g.
yarn workspaces run lint

# Run a command on a specific workspace
yarn workspace <package name> run <cmd>
# e.g.
yarn workspace @evaka/lib-common run test

# Or just use the scripts directly in the subdirectories
```

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
The rules are a Yarn workspace, so they can be used in a project by:

1. Adding a dependency to `@evaka/eslint-plugin`, example:

    ```json
    "devDependencies": {
      "@evaka/eslint-plugin": "0.0.1",
    ```

1. And including the plugin ruleset in ESLint configs (`package.json`), example:

    ```json
    "eslintConfig": {
      "extends": [
        // ...
        "plugin:@evaka/recommended"
      ],
    ```

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

### Production builds

Production build compiles the application into a minimized and optimized
version, that can then be served for the users through static file
hosting.

To create a production build, run

```bash
yarn build
```

**NOTE:** All application configuration should be made in packages'
config files, and only build-time configuration should be placed in
`.env*` files. The CI only creates a single build for _all_
environments (per package).

This allows us to make the CI loop shorter by avoiding duplicate
builds, and also to use complex types in configs (instead of strings).

### Overriding backend URLs

When running any local development servers, set the `API_PROXY_URL` to
override the default API proxy URL.
This is useful if you want to use the [`compose-e2e` stack](compose/README.md) for running the backend services.

### Yarn workspaces

This repository uses [Yarn workspaces](https://yarnpkg.com/lang/en/docs/workspaces/)
to share common code between packages / applications.

Following "simple" setup that enables typescript builds (both the
production code under `packages/` and Testcafe code under `e2e-test/`)
and IDEs to work seamlessly together:

- Yarn workspaces
  - Modules depending on other **source modules** define source modules
    as dependencies in their `package.json`
  - **IMPORTANT**: internal module version must match throughout the
    repository (e.g. always use exact version of `1.0.0`)
  - **NOTE:** If you want to use modules with tsconfig paths configured
    from end-to-end tests,
    you need to remove the path configurations, as Testcafe (as of 2019-10-24) does not support custom path configs
- Tsconfig files; the following files are used

  - `packages/tsconfig.json`

    - Root configuration for Typescript, that contains the typical ts
      compiler settings

  - `packages/*/tsconfig.json`
    - Package specific configuration for Typescript, that extend
      the root configuration and
      - Define where the package's sources are
      - Define package `outDir` for generated files (we don't want to
        accidentally mix generated sources with the actual ones)
  - `packages/*/tsconfig.test.json`
    - Package specific Typescript configurations for unit tests, that
      extend package configuration

### Caching assets

**NOTE:** Only concerns Vue builds.

By including hashes in asset filenames, they can be safely cached forever:

- You can be sure the latest version of the asset is always loaded
- Filenames are (relatively) guaranteed to be non-conflicting ->
  caching the asset with the hashed name is safe

To allow Webpack (used by Vue CLI) to process and, as a result,
generate hashed filenames for static assets, they must be required in
source code — instead of being placed to `public/` directory.

For example:

```ts
return {
  url: require('@evaka/enduser-frontend/src/assets/markerMULTIPLE.png')
}
```

results in Webpack generating a `dist/img/markerMULTIPLE.abc123asd.png` file in the build,
and replacing the reference with the hashed filename.

## Error logging and monitoring

eVaka has a support for using [Sentry](https://sentry.io) for error
logging in the frontend applications. Sentry's application monitoring
platform is a commercial product, that is not free to use.
**Please note, that you can run the application also without Sentry.**

### How to use Sentry

1. Create a project in Sentry's UI
1. Install all required dependencies:
   - `@sentry/browser` for everything
   - `@sentry/webpack-plugin` (devDependency) for automatically versioning and publishing source maps via Webpack
   - `@sentry/integrations` for Vue apps
1. Create `.sentryclirc` in the application's root directory:

   ```ini
   [defaults]
   project=evaka-employee-frontend
   org=acme-organisation
   ```

1. Follow Sentry's own documentation for initializing Sentry

   - Usually just (before initializing Vue or React):

     ```ts
     import * as Sentry from '@sentry/browser'
     import { getEnvironment } from '@evaka/lib-common/src/utils/helpers'
     import { config } from '@evaka/employee-frontend/src/configs'

     Sentry.init({
       enabled: config.sentry.enabled,
       dsn: config.sentry.dsn,
       environment: getEnvironment()
     })
     ```

   - _NOTE:_ Version number is automatically injected by the Webpack plugin

1. And lastly, configure the Webpack plugin (either in `vue.config.js` or `webpack.config.js`):

   ```js
   const SentryWebpackPlugin = require('@sentry/webpack-plugin')

   module.exports = function (env, argv) {
     const plugins = []

     if (process.env.SENTRY_PUBLISH_ENABLED) {
       // NOTE: Important to set urlPrefix correctly to match the deploy environment
       plugins.push(new SentryWebpackPlugin({ include: './dist', urlPrefix: '~/employee/' }))
     }
   // ...
   ```

1. CI automatically sets `SENTRY_PUBLISH_ENABLED=true` when building a `master` branch build
   - Avoid extraneous releases from non-master builds and local builds
   - CI is configured with `SENTRY_AUTH_TOKEN`
     - Auth token permissions:
       - `project:read`
       - `project:releases`
       - `org:read`

## Future improvements

- When Testcafe starts supporting custom compiler options (<https://github.com/DevExpress/testcafe/issues/1845>),
  we can properly start referencing package sources (typings, utils) in
  E2E tests, which will reduce duplication
  - Also requires <https://github.com/DevExpress/testcafe/issues/4405> to
    be released (`1.6.0` is unusable with TS)
