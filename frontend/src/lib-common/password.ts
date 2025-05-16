// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { PasswordConstraints } from './generated/api-types/shared'

export function isPasswordStructureValid(
  constraints: PasswordConstraints,
  password: string
) {
  function count(iter: Iterator<RegExpMatchArray>) {
    let result = iter.next()
    let count = 0
    while (!result.done) {
      count += 1
      result = iter.next()
    }
    return count
  }
  if (password.length < constraints.minLength) return false
  if (password.length > constraints.maxLength) return false
  // \p{...} check unicode categories: https://unicode.org/reports/tr18/#General_Category_Property
  if (count(password.matchAll(/\p{Ll}/gu)) < constraints.minLowers) return false
  if (count(password.matchAll(/\p{Lu}/gu)) < constraints.minUppers) return false
  if (count(password.matchAll(/\p{Nd}/gu)) < constraints.minDigits) return false
  if (count(password.matchAll(/[^\p{L}\p{N}]/gu)) < constraints.minSymbols)
    return false
  return true
}
