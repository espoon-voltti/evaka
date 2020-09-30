<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# @evaka/lib-common

Common TypeScript modules for all eVaka frontends.
Does not contain anything React or Vue specific, those modules
should be placed into their own libraries.

## Usage

Add `@evaka/lib-common` as a dependency in your `package.json`
**with a matching version number** in both `package.json` files
(this library's and the app's) -- that's how Yarn workspaces matches
the library to this local one. Specifying a mismatching version number
will result in an attempt to resolve the package in an npm registry.
