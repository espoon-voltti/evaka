// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { RuleTester } from 'eslint'

import noPagePause from './no-page-pause.js'

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
