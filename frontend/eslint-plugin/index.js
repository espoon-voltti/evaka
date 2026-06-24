// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import noIconsImports from './rules/no-icons-imports.js'
import noLocalDateTripleEquals from './rules/no-localdate-triple-equals.js'
import noPagePause from './rules/no-page-pause.js'
import noRelativeLibImports from './rules/no-relative-lib-imports.js'
import noTestOnly from './rules/no-testonly.js'
import shortestRelativeImports from './rules/shortest-relative-imports.js'

const plugin = {
  name: '@evaka/eslint-plugin',
  rules: {
    'no-page-pause': noPagePause,
    'no-testonly': noTestOnly,
    'no-relative-lib-imports': noRelativeLibImports,
    'no-icons-imports': noIconsImports,
    'no-localdate-triple-equals': noLocalDateTripleEquals,
    'shortest-relative-imports': shortestRelativeImports
  },
  configs: {}
}

export default plugin
