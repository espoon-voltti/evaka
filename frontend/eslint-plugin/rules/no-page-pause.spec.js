// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

const { RuleTester } = require('eslint')

const noPagePause = require('./no-page-pause')

new RuleTester().run('no-page-pause', noPagePause, {
  valid: [
    {
      code: 'await page.click()'
    }
  ],
  invalid: [
    {
      code: 'await page.pause()',
      errors: [{ message: 'Use of page.pause()' }]
    }
  ]
})
