// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { RuleTester } from 'eslint'

import noTestOnly from './no-testonly.js'

new RuleTester().run('no-testonly', noTestOnly, {
  valid: [
    {
      code: 'test("foo", () => {})'
    },
    {
      code: 'it("foo", () => {})'
    }
  ],
  invalid: [
    {
      code: 'describe.only("foo", () => {})',
      errors: [{ message: 'Use of `.only`' }]
    },
    {
      code: 'test.only("foo", () => {})',
      errors: [{ message: 'Use of `.only`' }]
    },
    {
      code: 'it.only("foo", () => {})',
      errors: [{ message: 'Use of `.only`' }]
    },
    {
      code: 'describe.each(e)("foo (%s)", (env) => { it.only("bar", () => {}) })',
      errors: [{ message: 'Use of `.only`' }]
    }
  ]
})
