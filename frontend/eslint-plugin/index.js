// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

module.exports = {
  rules: {
    'no-testonly': require('./rules/no-testonly')
  },
  configs: {
    recommended: {
      plugins: ['@evaka'],
      rules: {
        '@evaka/no-testonly': 'error'
      }
    }
  }
}
