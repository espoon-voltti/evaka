// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { RuleTester } from 'eslint'

import noIconsImports from './no-icons-imports'

new RuleTester().run('no-icons-imports', noIconsImports, {
  valid: [
    {
      code: 'import { foo } from "lib-icons"'
    }
  ],
  invalid: [
    {
      code: 'import { foo } from "Icons"',
      output: 'import { foo } from "lib-icons"',
      errors: [{ message: 'Importing from `Icons` is not allowed' }]
    }
  ]
})
