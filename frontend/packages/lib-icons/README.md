<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# @evaka/lib-icons

Fontawesome icons for eVaka frontends.

## Usage

Add `@evaka/lib-icons` as a dependency in your `package.json`
**with a matching version number** in both `package.json` files
(this library's and the app's) -- that's how Yarn workspaces matches
the library to this local one. Specifying a mismatching version number
will result in an attempt to resolve the package in an npm registry.

Since eVaka has the configuration option to use pro version of Fontawesome
icons you will have to add the following to your webpack configuration:
```js
{
  ...,
  resolve: {
    ...,
    alias: {
      'Icons': process.env.ICONS === 'pro'
        ? path.resolve(__dirname, '../lib-icons/pro-icons')
        : path.resolve(__dirname, '../lib-icons/free-icons')
    }
  }
}
```
You can use the [employee-frontend configuration](../employee-frontend/webpack.config.js)
as an example.
