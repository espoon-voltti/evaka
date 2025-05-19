// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

const plugin = {
  name: '@evaka/eslint-plugin',
  rules: {
    'no-page-pause': require('./rules/no-page-pause'),
    'no-testonly': require('./rules/no-testonly'),
    'no-relative-lib-imports': require('./rules/no-relative-lib-imports'),
    'no-icons-imports': require('./rules/no-icons-imports'),
    'no-localdate-triple-equals': require('./rules/no-localdate-triple-equals'),
    'shortest-relative-imports': require('./rules/shortest-relative-imports')
  },
  configs: {}
}

Object.assign(plugin.configs, {
  recommended: {
    plugins: {
      '@evaka': plugin
    },
    rules: {
      '@evaka/no-page-pause': 'error',
      '@evaka/no-testonly': 'error',
      '@evaka/no-relative-lib-imports': 'error',
      '@evaka/no-icons-imports': 'error',
      '@evaka/no-localdate-triple-equals': 'error',
      '@evaka/shortest-relative-imports': 'error'
    }
  }
})

module.exports = plugin
