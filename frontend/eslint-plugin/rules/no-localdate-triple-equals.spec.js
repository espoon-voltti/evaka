// SPDX-FileCopyrightText: 2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { RuleTester } from 'eslint'
import typescriptEslint from 'typescript-eslint'

import rule from './no-localdate-triple-equals'

const ruleTester = new RuleTester({
  languageOptions: {
    parser: typescriptEslint.parser,
    parserOptions: {
      ecmaVersion: 2022,
      sourceType: 'module'
    }
  }
})

ruleTester.run('no-localdate-triple-equals', rule, {
  valid: [
    {
      code: `
        import LocalDate from './local-date'
        const date1 = LocalDate.of(2024, 1, 1)
        const date2 = LocalDate.of(2024, 1, 1)
        if (date1.isEqual(date2)) {}
        if ('asd' === 'foober') {}
      `
    },
    {
      code: `
        const num1 = 1
        const num2 = 1
        if (num1 === num2) {}
      `
    },
    {
      code: `
        import LocalDate from './local-date'
        function foo(param: LocalDate | undefined) {
          return (param === undefined)
        }
      `
    },
    {
      code: `
        import LocalDate from './local-date'
        const isInSameTerm = (
          date1: LocalDate,
          date2: LocalDate,
          terms: FiniteDateRange[]
        ) => {
          const term1 = terms.find((term) => term.includes(date1))
          const term2 = terms.find((term) => term.includes(date2))
          return term1 === term2
        }
      `
    }
  ],
  invalid: [
    {
      code: `
        import LocalDate from './local-date'
        const date1 = LocalDate.of(2024, 1, 1)
        const date2 = LocalDate.of(2024, 1, 1)
        if (date1 === date2) {}
      `,
      errors: [
        {
          messageId: 'noTripleEquals'
        }
      ]
    },
    {
      code: `
        import LocalDate from './local-date'
        const date1 = LocalDate.of(2024, 1, 1)
        function foo(param: LocalDate) {
          const date2 = LocalDate.of(2024, 1, 1)
          return (param === date2)
        }
        foo(date1)
      `,
      errors: [
        {
          messageId: 'noTripleEquals'
        }
      ]
    },
    {
      code: `
        import LocalDate from './local-date'
        const validator = (preferredStartDate: LocalDate | null) =>
          (val: LocalDate | null): string | undefined =>
            val && preferredStartDate && (val === preferredStartDate)
              ? undefined
              : 'error'
      `,
      errors: [
        {
          messageId: 'noTripleEquals'
        }
      ]
    },
    {
      code: `
        import LocalDate from './local-date'
        describe('LocalDate', () => {
          it('supports comparisons', () => {
            const middle = LocalDate.of(2020, 7, 7)
            const duplicate = LocalDate.parseIso(middle.formatIso())
            expect(duplicate === middle).toBeFalsy()
          })
        })
      `,
      errors: [
        {
          messageId: 'noTripleEquals'
        }
      ]
    }
  ]
})
