// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { RuleTester } from 'eslint'

import noRelativeLibImports from './no-relative-lib-imports.js'

new RuleTester().run('no-relative-lib-imports', noRelativeLibImports, {
  valid: [
    {
      code: 'import { foo } from "lib-common/utils"'
    }
  ],
  invalid: [
    {
      code: 'import { foo } from "./lib-common/utils"',
      output: 'import { foo } from "lib-common/utils"',
      errors: [{ message: 'Relative `lib-*` imports are not allowed' }]
    },
    {
      code: 'import { foo } from "../../lib-common/utils"',
      output: 'import { foo } from "lib-common/utils"',
      errors: [{ message: 'Relative `lib-*` imports are not allowed' }]
    }
  ]
})
