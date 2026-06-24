// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { defineConfig } from 'oxfmt'

export default defineConfig({
  ignorePatterns: [
    'node_modules',
    '.yarn',
    'dist',
    'src/*/generated',
    'test-results'
  ],

  printWidth: 80,
  semi: false,
  singleQuote: true,
  trailingComma: 'none',
  arrowParens: 'always',
  sortImports: {
    groups: [
      'builtin',
      'external',
      'internal',
      'parent',
      'sibling',
      'index',
      'unknown'
    ],
    order: 'asc',
    ignoreCase: false,
    newlinesBetween: true,
    internalPattern: [
      'citizen-frontend',
      'e2e-test',
      'employee-frontend',
      'employee-mobile-frontend',
      'lib-common',
      'lib-components',
      'lib-customizations',
      'lib-icons',
      'maintenance-page-frontend'
    ]
  }
})
